package br.com.zup.endpoint

import br.com.zup.ConfirmaExclusaoResponse
import br.com.zup.ExclusaoChavePixRequest
import br.com.zup.ExclusaoChavePixServiceGrpc
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.erp.ItauErpClient
import br.com.zup.utils.extensions.validate
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.validation.validator.Validator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import javax.validation.ConstraintViolationException

@Singleton
class ExclusaoChavePixEndpoint(
    val repository: ChavePixRepository,
    val itauErpClient: ItauErpClient,
    val validator: Validator
) : ExclusaoChavePixServiceGrpc.ExclusaoChavePixServiceImplBase() {

    private val logger = LoggerFactory.getLogger(ExclusaoChavePixEndpoint::class.java)

    override fun excluiChave(request: ExclusaoChavePixRequest, responseObserver: StreamObserver<ConfirmaExclusaoResponse>) {

        try {

            // 1. valido o input
            logger.info("Requisição de exclusão recebida")
            logger.info("Validando a requisição")
            val exclusaoChavePix = request.validate(validator)

            logger.info("Parâmetros de chave válidos")
            logger.info("Verificando a existência da chave Pix")

            // 2. vejo se a chave já existe
            if (!repository.existsByChavePix(request.chavePix)) {
                logger.error("Chave pix não encontrada")
                responseObserver.onError(Status.NOT_FOUND
                    .withDescription("chave pix ${exclusaoChavePix.chavePix} inexistente")
                    .asRuntimeException())
                return
            }

            // 3. consulto o ERP pra ver se o cliente existe
            val consulta = itauErpClient.consulta(exclusaoChavePix.clientId)

            if (consulta.code() == 404) {
                logger.error("Cliente não encontrado 404")
                responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Cliente ${exclusaoChavePix.clientId} não encontrado")
                    .asRuntimeException()
                )
                return
            }
            logger.info("Dados do cliente validados")

            // 4. consulto se a chave pix pertence ao cliente solicitante
            val chavePix = repository.findByChavePix(exclusaoChavePix.chavePix)
            if (exclusaoChavePix.clientId != chavePix.clientId) {
                logger.error("Chave pix não pertencente ao cliente informado")
                responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Chave pix ${exclusaoChavePix.chavePix} não pertencente ao solicitante")
                    .asRuntimeException()
                )
                return
            }

            // 5. tudo passou? apago a chave pix
            logger.info("Apagando a chave Pix")
            repository.delete(chavePix)
            logger.info("Chave pix apagada")
            responseObserver.onNext(ConfirmaExclusaoResponse.newBuilder()
                    .setMensagem("Chave pix excluída com sucesso!")
                .build())
            responseObserver.onCompleted()


        }
        // validações da bean validation
        catch (e: ConstraintViolationException) {

            for (message in e.constraintViolations) {
                logger.error("Erro de validação: ${message.message}")
            }

            responseObserver.onError(
                Status.INVALID_ARGUMENT
                .withDescription("dados de entrada inválidos")
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