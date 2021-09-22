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
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.validation.Validated
import io.micronaut.validation.validator.Validator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import javax.validation.ConstraintViolationException

@Validated @Singleton
class ChavePixEndpoint (val repository: ChavePixRepository,
                        val itauErpClient: ItauErpClient,
                        val bcbClient: BcbClient,
                        val validator: Validator
                        ) : DesafioPixServiceGrpc.DesafioPixServiceImplBase() {

    private val logger = LoggerFactory.getLogger(ChavePixEndpoint::class.java)

    override fun cadastra(request: ChavePixRequest, responseObserver: StreamObserver<ChavePixResponse>) {

        try {

            // 1. valido o input
            logger.info("Requisição de nova chave recebida")
            logger.info("Validando a requisição de chave")
            val novaChavePix = request.validate(validator)

            logger.info("Parâmetros de chave validados")
            logger.info("Verificando duplicidade de chave Pix")

            // 2. vejo se a chave já existe
            if (repository.existsByChavePix(request.chavePix)) {
                logger.error("Chave pix já cadastrada")
                responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("chave pix ${request.chavePix} existente")
                    .asRuntimeException())
                return
            }
            logger.info("Chave pix não existente. Validando dados do cliente")

            // 3. consulto o ERP pra ver se o cliente existe
            val consultaErp = itauErpClient.consulta(request.clientId)

            if (consultaErp.code() == 404) {
                logger.error("Cliente não encontrado 404")
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Cliente ${request.clientId} não encontrado")
                        .asRuntimeException()
                )
                return
            }
            logger.info("Dados do cliente validados. Comunicando com o BCB")

            // 4. cliente existe? chave pix válida? bora comunicar com o BCB
            val bcbKeyRequest = novaChavePix.toBcbRequest()
            println(bcbKeyRequest.toString())

            val consultaBcb = bcbClient.cadastraBcb(bcbKeyRequest)

            // 5. tudo passou? converto pro modelo e salvo a chave
            if (novaChavePix.tipoChave == TipoChave.CHAVE_ALEATORIA) {
                logger.info("Recebendo a chave aleatória criada pelo BCB")
                novaChavePix.chavePix = (consultaBcb.body() as CreatePixKeyResponse).key
            }
            else {
                logger.info("Chave pix validada pelo BCB")
            }

            val chavePix = novaChavePix.toModel()

            logger.info("Salvando a nova chave pix")
            repository.save(chavePix)

            logger.info("Chave pix salva com sucesso")

            responseObserver.onNext(ChavePixResponse.newBuilder().setPixId(chavePix.id!!).build())
            responseObserver.onCompleted()
        }

        // validações da bean validation
        catch (e: ConstraintViolationException) {

            for (message in e.constraintViolations) {
                logger.error("Erro de validação: ${message.message}")
            }

            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("dados de entrada inválidos")
                .asRuntimeException())
            return
        }

        // pego a exception caso o ERP esteja derrubado
        catch (e: HttpClientException) {
            var sistemaOffline = ""
            if (e.message!!.contains(":8082")) {
                sistemaOffline += "Sistema do BCB encontra-se indisponível."
            }
            if (e.message!!.contains(":9091")) {
                sistemaOffline += "ERP do Itaú encontra-se indisponível."
            }
            logger.error(sistemaOffline)
            responseObserver.onError(Status.INTERNAL
                .withDescription(sistemaOffline)
                .asRuntimeException())
            return
        }

        // pego a exception genérica
        catch (e: Exception) {
            logger.error("Exceção desconhecida")
            e.printStackTrace()
            responseObserver.onError(Status.INTERNAL
                .withDescription("Algo deu muito ruim")
                .asRuntimeException())
            return
        }

    }

}