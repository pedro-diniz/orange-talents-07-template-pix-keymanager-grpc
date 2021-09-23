package br.com.zup.endpoint

import br.com.zup.ConfirmaExclusaoResponse
import br.com.zup.ExclusaoChavePixRequest
import br.com.zup.ExclusaoChavePixServiceGrpc
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.bcb.BcbClient
import br.com.zup.service.erp.ItauErpClient
import br.com.zup.utils.exceptionhandler.exceptions.ChaveDeOutroClienteException
import br.com.zup.utils.exceptionhandler.exceptions.ChaveInexistenteException
import br.com.zup.utils.exceptionhandler.exceptions.ClienteNaoEncontradoException
import br.com.zup.utils.exceptionhandler.exceptions.DeleteProibidoException
import br.com.zup.utils.exceptionhandler.handler.ErrorAroundHandler
import br.com.zup.utils.extensions.validate
import io.grpc.stub.StreamObserver
import io.micronaut.validation.validator.Validator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
@ErrorAroundHandler
class ExclusaoChavePixEndpoint(
    val repository: ChavePixRepository,
    val itauErpClient: ItauErpClient,
    val bcbClient: BcbClient,
    val validator: Validator
) : ExclusaoChavePixServiceGrpc.ExclusaoChavePixServiceImplBase() {

    private val logger = LoggerFactory.getLogger(ExclusaoChavePixEndpoint::class.java)

    override fun excluiChave(
        request: ExclusaoChavePixRequest,
        responseObserver: StreamObserver<ConfirmaExclusaoResponse>
    ) {

        // 1. valido o input
        logger.info("Requisição de exclusão recebida")
        logger.info("Validando a requisição")
        val exclusaoChavePix = request.validate(validator)

        logger.info("Parâmetros de chave válidos")
        logger.info("Verificando a existência da chave Pix")

        // 2. vejo se a chave já existe
        if (!repository.existsByChavePix(request.chavePix)) {
            logger.error("Chave pix não encontrada")
            throw ChaveInexistenteException("chave pix ${exclusaoChavePix.chavePix} inexistente")
        }
        logger.info("Chave pix encontrada. Validando dados do cliente.")

        // 3. consulto o ERP pra ver se o cliente existe
        val consulta = itauErpClient.consulta(exclusaoChavePix.clientId)
        logger.info("Passei da consulta?")

        if (consulta.code() == 404) {
            logger.error("Cliente não encontrado")
            throw ClienteNaoEncontradoException("Cliente ${exclusaoChavePix.clientId} não encontrado")
        }
        logger.info("Dados do cliente validados")

        // 4. consulto se a chave pix pertence ao cliente solicitante
        val chavePix = repository.findByChavePix(exclusaoChavePix.chavePix)
        if (exclusaoChavePix.clientId != chavePix.clientId) {
            logger.error("Chave pix não pertencente ao cliente informado")
            throw ChaveDeOutroClienteException("Chave pix ${exclusaoChavePix.chavePix} não pertencente ao solicitante")
        }

        logger.info("Chave pix pertence ao solicitante. Solicitando deleção ao o BCB")
        // 5. a chave existe e o solicitante é o dono? bora comunicar com o BCB
        val bcbDeleteRequest = exclusaoChavePix.toBcbRequest()

        val consultaBcb = bcbClient.apagaBcb(exclusaoChavePix.chavePix, bcbDeleteRequest)

        if (consultaBcb.code() == 403) {
            logger.error("Proibido realizar a operação")
            throw DeleteProibidoException("Proibido realizar a operação de deleção da chave ${exclusaoChavePix.chavePix}")
        }

        // 6. tudo passou? apago a chave pix
        logger.info("Deleção de chave autorizada pelo BCB. Apagando a chave pix")
        repository.delete(chavePix)
        logger.info("Chave pix apagada")
        responseObserver.onNext(
            ConfirmaExclusaoResponse.newBuilder()
                .setMensagem("Chave pix excluída com sucesso!")
                .build()
        )
        responseObserver.onCompleted()

    }

}