package br.com.zup.endpoint

import br.com.zup.*
import br.com.zup.endpoint.dto.request.ConsultaChavePixExternaDto
import br.com.zup.endpoint.dto.request.ConsultaChavePixInternaDto
import br.com.zup.model.ChavePix
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.bcb.BcbClient
import br.com.zup.service.bcb.dto.response.PixKeyDetailResponse
import br.com.zup.service.erp.ItauErpClient
import br.com.zup.utils.convertToProtobufTimestamp
import br.com.zup.utils.exceptionhandler.exceptions.ChaveDeOutroClienteException
import br.com.zup.utils.exceptionhandler.exceptions.ChaveInexistenteException
import br.com.zup.utils.exceptionhandler.exceptions.ClienteNaoEncontradoException
import br.com.zup.utils.exceptionhandler.handler.ErrorAroundHandler
import br.com.zup.utils.extensions.validate
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpResponse
import io.micronaut.validation.validator.Validator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*

@Singleton @ErrorAroundHandler
class ConsultaChavePixEndpoint (val repository: ChavePixRepository,
                                   val itauErpClient: ItauErpClient,
                                   val bcbClient: BcbClient,
                                   val validator: Validator
) : ConsultaChavePixServiceGrpc.ConsultaChavePixServiceImplBase() {

    private val logger = LoggerFactory.getLogger(ConsultaChavePixEndpoint::class.java)

    lateinit var chavePixLocal : ChavePix
    lateinit var consultaBcb : HttpResponse<PixKeyDetailResponse?>
    lateinit var chavePixConsultada : String

    var possivelChavePix : Optional<ChavePix> = Optional.empty()

    override fun consultaChave(
        request: ConsultaChavePixRequest,
        responseObserver: StreamObserver<ConsultaChavePixResponse>
    ) {

        // 1. valido o input
        logger.info("Requisição de consulta recebida")
        logger.info("Validando a requisição")
        val solicitaoConsulta = request.validate(validator)

        logger.info("Parâmetros de chave válidos")
        logger.info("Verificando a existência da chave Pix")

        // 2. vejo se a consulta é para uma chave interna ou externa
        if (solicitaoConsulta is ConsultaChavePixInternaDto) {
            logger.info("Requisição de consulta interna")

            logger.info(solicitaoConsulta.toString())
            // 2.1. vejo se a chave existe
            when {
                solicitaoConsulta.chavePix.isNullOrBlank() -> { // pesquisa por pixId
                    possivelChavePix = repository.findById(solicitaoConsulta.pixId)
                    if (possivelChavePix.isEmpty) {
                        logger.error("Chave pix não encontrada localmente")
                        throw ChaveInexistenteException("registro com Pix ID ${solicitaoConsulta.pixId} inexistente")
                    }
                }
                else -> { // pesquisa por chavePix
                    possivelChavePix = repository.findByChavePix(solicitaoConsulta.chavePix)
                    if (possivelChavePix.isEmpty) {
                        logger.error("Chave pix não encontrada localmente")
                    }
                }
            }

            // 2.2. encontrei a chave?
            if (possivelChavePix.isPresent) {

                logger.info("Chave pix encontrada. Validando dados do cliente.")
                chavePixLocal = possivelChavePix.get()

                // 2.3. consulto o ERP pra ver se o cliente existe
                val consultaErp = itauErpClient.consulta(solicitaoConsulta.clientId)

                if (consultaErp.code() == 404) {
                    logger.error("Cliente não encontrado")
                    throw ClienteNaoEncontradoException("Cliente ${solicitaoConsulta.clientId} não encontrado")
                }
                logger.info("Dados do cliente validados")

                // 2.4. consulto se a chave pix pertence ao cliente solicitante
                if (possivelChavePix.isPresent && solicitaoConsulta.clientId != chavePixLocal.clientId) {
                    logger.error("Chave pix não pertencente ao cliente informado")
                    throw ChaveDeOutroClienteException("Chave pix ${chavePixLocal.chavePix} não pertencente ao solicitante")
                }

                // 2.5. a chave existe e o solicitante é o dono? bora consultar no o BCB
                chavePixConsultada = chavePixLocal.chavePix
            }

        }
        else if (solicitaoConsulta is ConsultaChavePixExternaDto) {
            possivelChavePix = Optional.empty()
            logger.info("Requisição de consulta externa")

            chavePixConsultada = solicitaoConsulta.chavePix
        }

        consultaBcb = bcbClient.consultaBcb(chavePixConsultada)

        if (consultaBcb.code() == 404) {
            logger.error("Chave pix não encontrada")
            throw ChaveInexistenteException("chave pix $chavePixConsultada inexistente no BCB")
        }
        logger.info("Chave pix encontrada no BCB")

        // 3. tudo passou? devolvo a chave pix
        val pixKeyDetails = consultaBcb.body()!!

        val pixGrpcResponse = ConsultaChavePixResponse.newBuilder().let {

            if (possivelChavePix.isPresent) {
                it.setPixId(chavePixLocal.id!!)
                it.setClientId(chavePixLocal.clientId)
            }

            it.setKeyType(pixKeyDetails.keyType.toString())
            it.setKey(pixKeyDetails.key)
            it.setBankAccount(BankAccount.newBuilder()
                .setParticipant(pixKeyDetails.bankAccount.participant)
                .setBranch(pixKeyDetails.bankAccount.branch)
                .setAccountNumber(pixKeyDetails.bankAccount.accountNumber)
                .setAccountType(pixKeyDetails.bankAccount.accountType.toString()))
            it.setOwner(Owner.newBuilder()
                .setType(pixKeyDetails.owner.type.toString())
                .setName(pixKeyDetails.owner.name)
                .setTaxIdNumber(pixKeyDetails.owner.taxIdNumber))
            it.setCreatedAt(convertToProtobufTimestamp(pixKeyDetails.createdAt))
        }
            .build()

        responseObserver.onNext(pixGrpcResponse)
        responseObserver.onCompleted()

    }

}