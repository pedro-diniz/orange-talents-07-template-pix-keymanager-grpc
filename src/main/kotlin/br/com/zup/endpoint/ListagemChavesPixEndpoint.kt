package br.com.zup.endpoint

import br.com.zup.ListagemChavesPixRequest
import br.com.zup.ListagemChavesPixResponse
import br.com.zup.ListagemChavesPixServiceGrpc
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.erp.ItauErpClient
import br.com.zup.utils.exceptionhandler.exceptions.ClienteNaoEncontradoException
import br.com.zup.utils.exceptionhandler.handler.ErrorAroundHandler
import br.com.zup.utils.extensions.converteParaGrpc
import br.com.zup.utils.extensions.validate
import io.grpc.stub.StreamObserver
import io.micronaut.validation.validator.Validator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton @ErrorAroundHandler
class ListagemChavesPixEndpoint(val repository: ChavePixRepository,
                                val itauErpClient: ItauErpClient,
                                val validator: Validator
) : ListagemChavesPixServiceGrpc.ListagemChavesPixServiceImplBase() {

    private val logger = LoggerFactory.getLogger(ListagemChavesPixEndpoint::class.java)

    override fun listaChaves(
        request: ListagemChavesPixRequest,
        responseObserver: StreamObserver<ListagemChavesPixResponse>
    ) {

        // 1. valido o input
        logger.info("Requisição de listagem recebida")
        logger.info("Validando o ID do cliente")
        val solicitaoListagem = request.validate(validator)


        // 2. consulto o ERP pra ver se o cliente existe
        logger.info("ID do cliente validado. Pesquisando...")
        val consultaErp = itauErpClient.consulta(solicitaoListagem.clientId)

        if (consultaErp.code() == 404) {
            logger.error("Cliente não encontrado")
            throw ClienteNaoEncontradoException("Cliente ${solicitaoListagem.clientId} não encontrado")
        }

        // 3. cliente valido e encontrado? bora listar
        logger.info("Cliente encontrado.")

        val listaChaves = repository.findByClientId(solicitaoListagem.clientId)
        logger.info("Lista de chaves encontrada com sucesso")

        val listaChavesResponse = converteParaGrpc(listaChaves)

        logger.info("Lista de chaves processada com sucesso")
        responseObserver.onNext(listaChavesResponse)
        responseObserver.onCompleted()

    }

}