package br.com.zup.pix

import br.com.zup.ChavePixRequest
import br.com.zup.ChavePixResponse
import br.com.zup.DesafioPixServiceGrpc
import br.com.zup.erp.ItauErpClient
import br.com.zup.extensions.toModel
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.validation.Validated
import io.micronaut.validation.validator.Validator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import javax.validation.ConstraintViolationException

@Validated @Singleton
class ChavePixEndpoint (val repository: ChavePixRepository,
                        val itauErpClient: ItauErpClient,
                        val validator: Validator
                        ) : DesafioPixServiceGrpc.DesafioPixServiceImplBase() {

    private val logger = LoggerFactory.getLogger(ChavePixEndpoint::class.java)

    override fun cadastra(request: ChavePixRequest, responseObserver: StreamObserver<ChavePixResponse>) {

        try {

            // 1. valido o input
            logger.info("Requisição de nova chave recebida")
            val novaChavePix = request.toModel()
            logger.info("Validando a requisição de chave")
            validator.validate(novaChavePix).let {
                if (it.isNotEmpty()) {
                    throw ConstraintViolationException(it)
                }
            }
            logger.info("Parâmetros de chave válidos")
            logger.info("Verificando duplicidade de chave Pix")

            // 2. vejo se a chave já existe
            if (repository.existsByChavePix(request.chavePix)) {
                logger.error("Chave pix já cadastrada")
                responseObserver.onError(Status.ALREADY_EXISTS
                    .withDescription("chave pix \"${request.chavePix}\" existente")
                    .asRuntimeException())
                return
            }
            logger.info("Chave pix não existente. Validando dados do cliente")

            // 3. consulto o ERP pra ver se o cliente existe
            val consulta = itauErpClient.consulta(request.clientId)

            if (consulta.code() == 404) {
                logger.error("Cliente não encontrado 404")
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("cliente \"${request.clientId}\" não encontrado")
                        .asRuntimeException()
                )
                return
            }
            logger.info("Dados do cliente validados")

            // 4. tudo passou? converto pro modelo e salvo a chave
            val chavePix = novaChavePix.toModel()

            logger.info("Salvando a nova chave Pix")
            repository.save(chavePix)

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

        // pego a exception caso o cliente não exista
        catch (e: HttpClientResponseException) {
            logger.error("Cliente não encontrado")
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("cliente \"${request.clientId}\" não encontrado")
                .asRuntimeException())
            return
        }

        // pego a exception caso o ERP esteja derrubado
        catch (e: HttpClientException) {
            logger.error("Sistema ERP do Itaú não responde")
            responseObserver.onError(Status.INTERNAL
                .withDescription("ERP do Itaú encontra-se indisponível")
                .asRuntimeException())
            return
        }

        // pego a exception caso o ERP esteja derrubado
        catch (e: Exception) {
            logger.error("Exceção desconhecida")
            responseObserver.onError(Status.INTERNAL
                .withDescription("Algo deu muito ruim")
                .asRuntimeException())
            return
        }

    }

}