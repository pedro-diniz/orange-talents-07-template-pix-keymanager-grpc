package br.com.zup.endpoint

import br.com.zup.ChavePixRequest
import br.com.zup.DesafioPixServiceGrpc
import br.com.zup.TipoChave
import br.com.zup.TipoConta
import br.com.zup.service.erp.ItauErpClient
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.bcb.BcbClient
import br.com.zup.service.bcb.dto.AccountType
import br.com.zup.service.bcb.dto.KeyType
import br.com.zup.service.bcb.dto.OwnerType
import br.com.zup.service.bcb.dto.request.CreatePixKeyRequest
import br.com.zup.service.bcb.dto.response.BankAccountResponse
import br.com.zup.service.bcb.dto.response.CreatePixKeyResponse
import br.com.zup.service.bcb.dto.response.OwnerResponse
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
import java.time.LocalDateTime
import java.util.*

@MicronautTest(transactional = false)
internal class ChavePixEndpointTest(
    val grpcCliente: DesafioPixServiceGrpc.DesafioPixServiceBlockingStub,
    val chavePixRepository: ChavePixRepository
) {

    @Inject lateinit var itauErpClient: ItauErpClient
    @Inject lateinit var bcbClient: BcbClient

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

        val bcbRequest = CreatePixKeyRequest(TipoChave.EMAIL, "fulano@zup.com.br", TipoConta.CONTA_POUPANCA)

        val response = grpcCliente.cadastra(
            ChavePixRequest.newBuilder()
                .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
                .setTipoChave(TipoChave.EMAIL)
                .setChavePix("fulano@zup.com.br")
                .setTipoConta(TipoConta.CONTA_POUPANCA)
                .build()
        )

        Mockito.`when`(bcbClient.cadastraBcb(bcbRequest)).thenReturn(HttpResponse.ok())

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

        val bcbRequest = CreatePixKeyRequest(TipoChave.EMAIL, "fulano@zup.com.br", TipoConta.CONTA_POUPANCA)

        Mockito.`when`(bcbClient.cadastraBcb(bcbRequest)).thenReturn(HttpResponse.ok())

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

        val bcbRequest = CreatePixKeyRequest(TipoChave.EMAIL, "fulano@zup.com.br", TipoConta.CONTA_POUPANCA)

        Mockito.`when`(bcbClient.cadastraBcb(bcbRequest)).thenReturn(HttpResponse.ok())

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

        val bcbRequest = CreatePixKeyRequest(TipoChave.EMAIL, "fulano@zup.com.br", TipoConta.CONTA_POUPANCA)

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(HttpClientException(":9091"))
        Mockito.`when`(bcbClient.cadastraBcb(bcbRequest)).thenReturn(HttpResponse.ok())

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.cadastra(request)
        }

        with(error) {
            assertEquals(Status.INTERNAL.code, status.code)
            assertEquals("ERP do Itaú encontra-se indisponível.", status.description)
        }
    }

    @Test
    internal fun `nao deve cadastrar uma nova chave pix se o BCB estiver off`() {

        val request = ChavePixRequest.newBuilder()
            .setClientId("de95a228-1f27-4ad2-907e-e5a2d816e9bc")
            .setTipoChave(TipoChave.EMAIL)
            .setChavePix("fulano@zup.com.br")
            .setTipoConta(TipoConta.CONTA_POUPANCA)
            .build()

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(HttpClientException(":8082"))

        val bcbRequest = CreatePixKeyRequest(TipoChave.EMAIL, "fulano@zup.com.br", TipoConta.CONTA_POUPANCA)

        // não está lançando exceção quando eu mando um thenThrow aqui
        Mockito.`when`(bcbClient.cadastraBcb(bcbRequest)).thenReturn(HttpResponse.ok())

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.cadastra(request)
        }

        with(error) {
            assertEquals(Status.INTERNAL.code, status.code)
            assertEquals("Sistema do BCB encontra-se indisponível.", status.description)
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

        val bcbRequest = CreatePixKeyRequest(TipoChave.EMAIL, "fulano@zup.com.br", TipoConta.CONTA_POUPANCA)

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(RuntimeException())
        Mockito.`when`(bcbClient.cadastraBcb(bcbRequest)).thenReturn(HttpResponse.ok())

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.cadastra(request)
        }

        with(error) {
            assertEquals(Status.INTERNAL.code, status.code)
            assertEquals("Algo deu muito ruim", status.description)
        }

    }

    @Test
    internal fun `deve receber chave aleatoria do BCB`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())

        // nunca vai gerar os mesmos valores aleatorios que a keyRequest gera lá no endpoint.
            // preciso do Any aqui, de algum jeito.
        val bcbRequest = CreatePixKeyRequest(TipoChave.CHAVE_ALEATORIA, "", TipoConta.CONTA_POUPANCA)

        val response = grpcCliente.cadastra(
            ChavePixRequest.newBuilder()
                .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
                .setTipoChave(TipoChave.CHAVE_ALEATORIA)
                .setTipoConta(TipoConta.CONTA_POUPANCA)
                .build()
        )

        val bcbKeyResponse = CreatePixKeyResponse(
            keyType = KeyType.RANDOM,
            key = UUID.randomUUID().toString(),
            bankAccount = BankAccountResponse(
                "60701190",
                "0001",
                "123456",
                AccountType.CACC
            ),
            owner = OwnerResponse(
                OwnerType.NATURAL_PERSON,
                "Pedro Diniz",
                "12345678901"
            ),
            createdAt = LocalDateTime.now()
        )
        Mockito.`when`(bcbClient.cadastraBcb(bcbRequest)).thenReturn(HttpResponse.created(bcbKeyResponse))

        assertTrue(chavePixRepository.existsByChavePix(bcbKeyResponse.key))
    }

    @MockBean(ItauErpClient::class) // bean a ser mockado
    fun itauErpClient() : ItauErpClient {
        return Mockito.mock(ItauErpClient::class.java)
    }

    @MockBean(BcbClient::class) // bean a ser mockado
    fun bcbClient() : BcbClient {
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class Clients { // o Micronaut sempre levanta os testes em uma porta diferente. Para termos acesso a esse channel, usamos aquele enum no argumento
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): DesafioPixServiceGrpc.DesafioPixServiceBlockingStub? {
            return DesafioPixServiceGrpc.newBlockingStub(channel)
        }
    }

}