package br.com.zup.endpoint

import br.com.zup.ChavePixRequest
import br.com.zup.ChavePixResponse
import br.com.zup.DesafioPixServiceGrpc
import br.com.zup.TipoChave
import br.com.zup.service.erp.ItauErpClient
import br.com.zup.utils.extensions.validate
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.bcb.BcbClient
import br.com.zup.service.bcb.dto.response.CreatePixKeyResponse
import br.com.zup.utils.exceptionhandler.exceptions.ChaveExistenteException
import br.com.zup.utils.exceptionhandler.exceptions.ClienteNaoEncontradoException
import br.com.zup.utils.exceptionhandler.handler.ErrorAroundHandler
import io.grpc.stub.StreamObserver
import io.micronaut.validation.validator.Validator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
@ErrorAroundHandler
class ChavePixEndpoint(
    val repository: ChavePixRepository,
    val itauErpClient: ItauErpClient,
    val bcbClient: BcbClient,
    val validator: Validator
) : DesafioPixServiceGrpc.DesafioPixServiceImplBase() {

    private val logger = LoggerFactory.getLogger(ChavePixEndpoint::class.java)

    override fun cadastra(request: ChavePixRequest, responseObserver: StreamObserver<ChavePixResponse>) {


        // 1. valido o input
        logger.info("Requisição de nova chave recebida")
        logger.info("Validando a requisição de chave")
        val solicitacaoCadastro = request.validate(validator)

        logger.info("Parâmetros de chave validados")
        logger.info("Verificando duplicidade de chave Pix")

        // 2. vejo se a chave já existe
        if (!solicitacaoCadastro.chavePix.isNullOrBlank() && repository.existsByChavePix(solicitacaoCadastro.chavePix!!)) {
            logger.error("Chave pix já cadastrada")
            throw ChaveExistenteException("chave pix ${solicitacaoCadastro.chavePix} existente")
        }
        logger.info("Chave pix não existente. Validando dados do cliente")

        // 3. consulto o ERP pra ver se o cliente existe
        val consultaErp = itauErpClient.consulta(request.clientId)

        if (consultaErp.code() == 404) {
            logger.error("Cliente não encontrado")
            throw ClienteNaoEncontradoException("Cliente ${request.clientId} não encontrado")
        }
        logger.info("Dados do cliente validados. Comunicando com o BCB")

        // 4. cliente existe? chave pix válida? bora comunicar com o BCB
        val bcbKeyRequest = solicitacaoCadastro.toBcbRequest()
        println("bcbRequest do endpoint: ${bcbKeyRequest.toString()}")

        val consultaBcb = bcbClient.cadastraBcb(bcbKeyRequest)

        // 5. tudo passou? converto pro modelo e salvo a chave
        if (solicitacaoCadastro.tipoChave == TipoChave.CHAVE_ALEATORIA) {
            logger.info("Recebendo a chave aleatória criada pelo BCB")
            solicitacaoCadastro.chavePix = (consultaBcb.body() as CreatePixKeyResponse).key
        } else {
            logger.info("Chave pix validada pelo BCB")
        }

        val chavePix = solicitacaoCadastro.toModel()

        logger.info("Salvando a nova chave pix")
        repository.save(chavePix)

        logger.info("Chave pix salva com sucesso")

        responseObserver.onNext(ChavePixResponse.newBuilder().setPixId(chavePix.id!!).build())
        responseObserver.onCompleted()

    }

}