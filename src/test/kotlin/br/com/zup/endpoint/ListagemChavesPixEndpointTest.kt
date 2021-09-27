package br.com.zup.endpoint

import br.com.zup.ListagemChavesPixRequest
import br.com.zup.ListagemChavesPixServiceGrpc
import br.com.zup.TipoChave
import br.com.zup.TipoConta
import br.com.zup.model.ChavePix
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.erp.ItauErpClient
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

@MicronautTest(transactional = false)
internal class ListagemChavesPixEndpointTest(
    val grpcCliente: ListagemChavesPixServiceGrpc.ListagemChavesPixServiceBlockingStub,
    val chavePixRepository: ChavePixRepository
) {

    @Inject lateinit var itauErpClient: ItauErpClient

    @BeforeEach
    internal fun setUp() {
        chavePixRepository.deleteAll()

        val chavePixTest = ChavePix(
            clientId = "0d1bb194-3c52-4e67-8c35-a93c0af9284f",
            tipoChave = TipoChave.EMAIL,
            chavePix = "fulano@zup.com.br",
            tipoConta = TipoConta.CONTA_POUPANCA
        )

        val chavePixTest2 = ChavePix(
            clientId = "0d1bb194-3c52-4e67-8c35-a93c0af9284f",
            tipoChave = TipoChave.TELEFONE_CELULAR,
            chavePix = "+5584999777555",
            tipoConta = TipoConta.CONTA_CORRENTE
        )

        chavePixRepository.saveAll(mutableListOf(chavePixTest, chavePixTest2))
    }

    @AfterEach
    internal fun tearDown() {
        chavePixRepository.deleteAll()
    }

    @Test
    internal fun `deve trazer uma lista de chaves`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())

        val response = grpcCliente.listaChaves(ListagemChavesPixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .build())

        with(response) {
            assertEquals(2, listagemChavesPixResponseList.size)
        }
    }

    @Test
    internal fun `nao deve trazer uma lista de chaves se o cliente nao existir`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.notFound())

        val request = ListagemChavesPixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284e")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.listaChaves(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Cliente ${request.clientId} não encontrado", status.description)
        }

    }

    @Test
    internal fun `deve lancar excecao se clientId nao for valido`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.notFound())

        val request = ListagemChavesPixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0afzzzzz")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.listaChaves(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }
    }

    @Test
    internal fun `deve lancar excecao se ERP estiver off`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(HttpClientException(":9091"))

        val request = ListagemChavesPixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.listaChaves(request)
        }

        with(error) {
            assertEquals(Status.UNAVAILABLE.code, status.code)
            assertEquals("ERP do Itaú encontra-se indisponível", status.description)
        }
    }

    @MockBean(ItauErpClient::class) // bean a ser mockado
    fun itauErpClient() : ItauErpClient {
        return Mockito.mock(ItauErpClient::class.java)
    }

    @Factory
    class Clients { // o Micronaut sempre levanta os testes em uma porta diferente. Para termos acesso a esse channel, usamos aquele enum no argumento
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): ListagemChavesPixServiceGrpc.ListagemChavesPixServiceBlockingStub? {
            return ListagemChavesPixServiceGrpc.newBlockingStub(channel)
        }
    }
}