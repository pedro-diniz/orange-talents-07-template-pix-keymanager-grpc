package br.com.zup.endpoint

import br.com.zup.ChavePixRequest
import br.com.zup.DesafioPixServiceGrpc
import br.com.zup.TipoChave
import br.com.zup.TipoConta
import br.com.zup.service.erp.ItauErpClient
import br.com.zup.repository.ChavePixRepository
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
internal class ChavePixEndpointTest(
    val grpcCliente: DesafioPixServiceGrpc.DesafioPixServiceBlockingStub,
    val chavePixRepository: ChavePixRepository
) {

    @Inject lateinit var itauErpClient: ItauErpClient

    @BeforeEach
    internal fun setUp() {
        chavePixRepository.deleteAll()
    }

    @AfterEach
    internal fun tearDown() {
        chavePixRepository.deleteAll()
    }

    @Test
    internal fun `deve cadastrar uma nova chave pix`() {

        // não deveria precisar disso se soubesse usar os mocks só onde preciso
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())

        val response = grpcCliente.cadastra(
            ChavePixRequest.newBuilder()
                .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
                .setTipoChave(TipoChave.EMAIL)
                .setChavePix("fulano@zup.com.br")
                .setTipoConta(TipoConta.CONTA_POUPANCA)
                .build()
        )

        with(response) {
            assertNotNull(pixId)
            assertTrue(chavePixRepository.existsById(pixId))
        }
    }

    @Test
    internal fun `nao deve cadastrar uma chave pix se o cliente nao existir`() {

        // não deveria precisar disso se soubesse usar os mocks só onde preciso
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.notFound())

        val request = ChavePixRequest.newBuilder()
            .setClientId("0d073d52-1b03-11ec-9621-0242ac13000")
            .setTipoChave(TipoChave.EMAIL)
            .setChavePix("fulano@zup.com.br")
            .setTipoConta(TipoConta.CONTA_POUPANCA)
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.cadastra(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Cliente ${request.clientId} não encontrado", status.description)
        }

    }

    @Test
    internal fun `nao deve cadastrar uma chave duplicada`() {

        // não deveria precisar disso se soubesse usar os mocks só onde preciso
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())

        val request = ChavePixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setTipoChave(TipoChave.EMAIL)
            .setChavePix("fulano@zup.com.br")
            .setTipoConta(TipoConta.CONTA_POUPANCA)
            .build()

        val response = grpcCliente.cadastra(request)

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.cadastra(
                ChavePixRequest.newBuilder()
                    .setClientId("de95a228-1f27-4ad2-907e-e5a2d816e9bc")
                    .setTipoChave(TipoChave.EMAIL)
                    .setChavePix("fulano@zup.com.br")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("chave pix ${request.chavePix} existente", status.description)
        }
    }

    @Test
    internal fun `nao deve cadastrar uma chave pix com input invalido`() {
        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.cadastra(
                ChavePixRequest.newBuilder()
                    .setClientId("de95a228-1f27-4ad2-907e-e5a2d816e9bc")
                    .setTipoChave(TipoChave.TIPO_CHAVE_UNKNOWN)
                    .setChavePix("")
                    .setTipoConta(TipoConta.TIPO_CONTA_UNKNOWN)
                    .build()
            )
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }

    }

    @Test
    internal fun `nao deve cadastrar uma nova chave pix se o ERP estiver off`() {

        val request = ChavePixRequest.newBuilder()
            .setClientId("de95a228-1f27-4ad2-907e-e5a2d816e9bc")
            .setTipoChave(TipoChave.EMAIL)
            .setChavePix("fulano@zup.com.br")
            .setTipoConta(TipoConta.CONTA_POUPANCA)
            .build()

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(HttpClientException::class.java)

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.cadastra(request)
        }

        with(error) {
            assertEquals(Status.INTERNAL.code, status.code)
            assertEquals("ERP do Itaú encontra-se indisponível", status.description)
        }
    }

    @Test
    internal fun `deve lancar excecao desconhecida`() {

        val request = ChavePixRequest.newBuilder()
            .setClientId("de95a228-1f27-4ad2-907e-e5a2d816e9bc")
            .setTipoChave(TipoChave.EMAIL)
            .setChavePix("fulano@zup.com.br")
            .setTipoConta(TipoConta.CONTA_POUPANCA)
            .build()

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(RuntimeException())

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.cadastra(request)
        }

        with(error) {
            assertEquals(Status.INTERNAL.code, status.code)
            assertEquals("Algo deu muito ruim", status.description)
        }

    }

    @MockBean(ItauErpClient::class) // bean a ser mockado
    fun itauErpClient() : ItauErpClient {
        return Mockito.mock(ItauErpClient::class.java)
    }

    @Factory
    class Clients { // o Micronaut sempre levanta os testes em uma porta diferente. Para termos acesso a esse channel, usamos aquele enum no argumento
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): DesafioPixServiceGrpc.DesafioPixServiceBlockingStub? {
            return DesafioPixServiceGrpc.newBlockingStub(channel)
        }
    }

}