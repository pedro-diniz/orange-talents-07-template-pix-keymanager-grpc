package br.com.zup.endpoint

import br.com.zup.ConsultaChavePixRequest
import br.com.zup.ConsultaChavePixServiceGrpc
import br.com.zup.TipoChave
import br.com.zup.TipoConta
import br.com.zup.model.ChavePix
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.bcb.BcbClient
import br.com.zup.service.bcb.dto.AccountType
import br.com.zup.service.bcb.dto.KeyType
import br.com.zup.service.bcb.dto.OwnerType
import br.com.zup.service.bcb.dto.response.BankAccountResponse
import br.com.zup.service.bcb.dto.response.OwnerResponse
import br.com.zup.service.bcb.dto.response.PixKeyDetailResponse
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
import java.time.LocalDateTime

@MicronautTest(transactional = false)
internal class ConsultaChavePixEndpointTest(
    val grpcCliente: ConsultaChavePixServiceGrpc.ConsultaChavePixServiceBlockingStub,
    val chavePixRepository: ChavePixRepository
) {

    @Inject lateinit var itauErpClient: ItauErpClient
    @Inject lateinit var bcbClient: BcbClient

    val chavePixTest = ChavePix(
        clientId = "0d1bb194-3c52-4e67-8c35-a93c0af9284f",
        tipoChave = TipoChave.EMAIL,
        chavePix = "fulano@zup.com.br",
        tipoConta = TipoConta.CONTA_POUPANCA
    )

    val chavePixTest2 = ChavePix(
        clientId = "c56dfef4-7901-44fb-84e2-a2cefb157890",
        tipoChave = TipoChave.EMAIL,
        chavePix = "beltrano@zup.com.br",
        tipoConta = TipoConta.CONTA_POUPANCA
    )

    lateinit var bcbHttpResponse : HttpResponse<PixKeyDetailResponse?>

    @BeforeEach
    internal fun setUp() {

        bcbHttpResponse = HttpResponse.ok(
            PixKeyDetailResponse(
                KeyType.EMAIL,
                "fulano@zup.com.br",
                BankAccountResponse(
                    "60701190",
                    "0001",
                    "123456",
                    AccountType.SVGS
                ), OwnerResponse(
                    OwnerType.NATURAL_PERSON,
                    "Rafael Ponte",
                    "12345678909"
                ), LocalDateTime.now()
            )
        )

        chavePixRepository.saveAll(mutableListOf(chavePixTest, chavePixTest2))
    }

    @AfterEach
    internal fun tearDown() {
        chavePixRepository.deleteAll()
    }

    @Test
    internal fun `deve retornar um registro pelo pixId`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(bcbHttpResponse)

        val response = grpcCliente.consultaChave(
            ConsultaChavePixRequest.newBuilder()
                .setPixId(chavePixTest.id!!)
                .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
                .build()
        )

        println(response.toString())

        with(response) {
            assertEquals(chavePixTest.id, pixId)
            assertEquals("fulano@zup.com.br", key)
            assertEquals(TipoChave.EMAIL.toString(), keyType)
            assertEquals("0d1bb194-3c52-4e67-8c35-a93c0af9284f", clientId)
        }

    }

    @Test
    internal fun `deve retornar um registro pela chave pix`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(bcbHttpResponse)

        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(chavePixTest.id!!)
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setChavePix("sicrano@zup.com.br")
            .build()

        val response = grpcCliente.consultaChave(request)

        println(response.toString())

        with(response) {
            assertEquals("sicrano@zup.com.br", request.chavePix)
            assertEquals(TipoChave.EMAIL.toString(), keyType)
        }

    }

    @Test
    internal fun `nao deve retornar um registro pesquisando externo`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(HttpResponse.notFound())

        val request = ConsultaChavePixRequest.newBuilder()
            .setChavePix("fulano@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.consultaChave(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("chave pix ${request.chavePix} inexistente no BCB", status.description)
        }
    }

    @Test
    internal fun `nao deve retornar um registro pesquisando interno pelo pixId`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(bcbHttpResponse)

        val request = ConsultaChavePixRequest.newBuilder()
                .setPixId(100000)
                .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
                .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.consultaChave(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("registro com Pix ID ${request.pixId} inexistente", status.description)
        }

    }

    @Test
    internal fun `nao deve retornar um registro com input invalido`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(bcbHttpResponse)

        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(-1)
            .setClientId("quebrando tudo caraio")
            .setChavePix("fala galera")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.consultaChave(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }

    }

    @Test
    internal fun `nao deve retornar um registro se cliente nao existe`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.notFound())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(bcbHttpResponse)

        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(chavePixTest.id!!)
            .setClientId("44321dac-1f9e-11ec-9621-0242ac130002")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.consultaChave(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Cliente ${request.clientId} não encontrado", status.description)
        }

    }

    @Test
    internal fun `nao deve retornar um registro se chave nao pertence a cliente`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(bcbHttpResponse)

        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(chavePixTest.id!!)
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setChavePix("beltrano@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.consultaChave(request)
        }

        with(error) {
            assertEquals(Status.PERMISSION_DENIED.code, status.code)
            assertEquals("Chave pix ${request.chavePix} não pertencente ao solicitante", status.description)
        }

    }

    @Test
    internal fun `nao deve retornar um registro interno se ERP estiver off`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(HttpClientException(":9091"))
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(bcbHttpResponse)

        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(chavePixTest.id!!)
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.consultaChave(request)
        }

        with(error) {
            assertEquals(Status.UNAVAILABLE.code, status.code)
            assertEquals("ERP do Itaú encontra-se indisponível", status.description)
        }

    }

    @Test
    internal fun `nao deve retornar um registro externo se BCB estiver off`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenThrow(HttpClientException(":8082"))

        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(chavePixTest.id!!)
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.consultaChave(request)
        }

        with(error) {
            assertEquals(Status.UNAVAILABLE.code, status.code)
            assertEquals("Sistema do BCB encontra-se indisponível", status.description)
        }
    }

    @Test
    internal fun `nao deve retornar um registro se request eh montada de forma invalida`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.consultaBcb(Mockito.anyString())).thenReturn(bcbHttpResponse)

        val request = ConsultaChavePixRequest.newBuilder()
            .setPixId(chavePixTest.id!!)
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.consultaChave(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada não se encaixem em nenhuma modalidade de consulta", status.description)
        }
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
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): ConsultaChavePixServiceGrpc.ConsultaChavePixServiceBlockingStub? {
            return ConsultaChavePixServiceGrpc.newBlockingStub(channel)
        }
    }

}