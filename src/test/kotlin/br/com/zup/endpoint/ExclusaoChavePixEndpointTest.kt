package br.com.zup.endpoint

import br.com.zup.ExclusaoChavePixRequest
import br.com.zup.ExclusaoChavePixServiceGrpc
import br.com.zup.TipoChave
import br.com.zup.TipoConta
import br.com.zup.model.ChavePix
import br.com.zup.repository.ChavePixRepository
import br.com.zup.service.bcb.BcbClient
import br.com.zup.service.bcb.dto.request.CreatePixKeyRequest
import br.com.zup.service.bcb.dto.request.DeletePixKeyRequest
import br.com.zup.service.bcb.dto.response.DeletePixKeyResponse
import br.com.zup.service.erp.ItauErpClient
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatcher
import org.mockito.Mockito
import org.mockito.internal.matchers.InstanceOf
import org.mockito.internal.progress.ThreadSafeMockingProgress
import java.time.LocalDateTime

@MicronautTest(transactional = false)
internal class ExclusaoChavePixEndpointTest (
    val grpcCliente: ExclusaoChavePixServiceGrpc.ExclusaoChavePixServiceBlockingStub,
    val chavePixRepository: ChavePixRepository
) {

    @Inject
    lateinit var itauErpClient: ItauErpClient
    @Inject lateinit var bcbClient: BcbClient

    @BeforeEach
    internal fun setUp() {
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

        chavePixRepository.saveAll(mutableListOf(chavePixTest, chavePixTest2))
    }

    @AfterEach
    internal fun tearDown() {
        chavePixRepository.deleteAll()
    }

    @Test
    internal fun `deve apagar uma chave pix`() {

        val bcbHttpResponse : HttpResponse<DeletePixKeyResponse?> = HttpResponse.ok(
            DeletePixKeyResponse("fulano@zup.com.br", "60701190", LocalDateTime.now())
        )

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.apagaBcb(Mockito.anyString(), AnyPixKey.para("fulano@zup.com.br"))
        ).thenReturn(bcbHttpResponse)


        grpcCliente.excluiChave(
            ExclusaoChavePixRequest.newBuilder()
                .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
                .setChavePix("fulano@zup.com.br")
                .build()
        )

        assertFalse(chavePixRepository.existsByChavePix("fulano@zup.com.br"))
    }

    @Test
    internal fun `nao deve apagar chave pix se input for invalido`() {
        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.excluiChave(
                ExclusaoChavePixRequest.newBuilder()
                    .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284z")
                    .setChavePix("")
                    .build()
            )
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos", status.description)
        }
    }

    @Test
    internal fun `nao deve apagar uma chave pix se chave nao existe`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())

        val request = ExclusaoChavePixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setChavePix("fulanow@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.excluiChave(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("chave pix ${request.chavePix} inexistente", status.description)
        }

    }

    @Test
    internal fun `nao deve apagar chave pix se cliente nao existe`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.notFound())

        val request = ExclusaoChavePixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284e")
            .setChavePix("fulano@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.excluiChave(request)
        }

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Cliente ${request.clientId} não encontrado", status.description)
        }
    }

    @Test
    internal fun `nao deve apagar chave pix se chave nao pertence a cliente`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())

        val request = ExclusaoChavePixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setChavePix("beltrano@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.excluiChave(request)
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave pix ${request.chavePix} não pertencente ao solicitante", status.description)
        }
    }

    @Test
    internal fun `nao deve apagar chave pix se ERP estiver off`() {

        val bcbHttpResponse : HttpResponse<DeletePixKeyResponse?> = HttpResponse.ok(
            DeletePixKeyResponse("fulano@zup.com.br", "60701190", LocalDateTime.now())
        )

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(HttpClientException(":9091"))
        Mockito.`when`(bcbClient.apagaBcb(Mockito.anyString(), AnyPixKey.para("fulano@zup.com.br"))
        ).thenReturn(bcbHttpResponse)

        val request = ExclusaoChavePixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setChavePix("fulano@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.excluiChave(request)
        }

        with(error) {
            assertEquals(Status.UNAVAILABLE.code, status.code)
            assertEquals("ERP do Itaú encontra-se indisponível", status.description)
        }
    }

    @Test
    internal fun `nao deve apagar chave pix se BCB estiver off`() {

        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.apagaBcb(Mockito.anyString(), AnyPixKey.para("fulano@zup.com.br"))
        ).thenThrow(HttpClientException(":8082"))

        val request = ExclusaoChavePixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setChavePix("fulano@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.excluiChave(request)
        }

        with(error) {
            assertEquals(Status.UNAVAILABLE.code, status.code)
            assertEquals("Sistema do BCB encontra-se indisponível", status.description)
        }
    }

    @Test
    internal fun `deve lancar excecao desconhecida`() {
        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenThrow(RuntimeException())

        val request = ExclusaoChavePixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setChavePix("beltrano@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.excluiChave(request)
        }

        with(error) {
            assertEquals(Status.INTERNAL.code, status.code)
            assertEquals("Algo deu muito ruim", status.description)
        }
    }

    @Test
    internal fun `nao deve deletar chave pix se bcb retornar 403`() {

        val bcbHttpResponse : HttpResponse<DeletePixKeyResponse?> = HttpResponse.status(HttpStatus.FORBIDDEN)


        Mockito.`when`(itauErpClient.consulta(Mockito.anyString())).thenReturn(HttpResponse.ok())
        Mockito.`when`(bcbClient.apagaBcb(Mockito.anyString(), AnyPixKey.para("fulano@zup.com.br"))
        ).thenReturn(bcbHttpResponse)


        val request = ExclusaoChavePixRequest.newBuilder()
            .setClientId("0d1bb194-3c52-4e67-8c35-a93c0af9284f")
            .setChavePix("fulano@zup.com.br")
            .build()

        val error = assertThrows<StatusRuntimeException> {
            grpcCliente.excluiChave(request)
        }

        with(error) {
            assertEquals(Status.PERMISSION_DENIED.code, status.code)
            assertEquals("Proibido realizar a operação de deleção da chave ${request.chavePix}", status.description)
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
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): ExclusaoChavePixServiceGrpc.ExclusaoChavePixServiceBlockingStub? {
            return ExclusaoChavePixServiceGrpc.newBlockingStub(channel)
        }
    }

    // nome do objeto e como ele será chamado -> AnyPixKey.para(TipoChave.CPF))
    object AnyPixKey {

        // gera um objeto genérico CreatePixKeyRequest com uma conta poupanca e o tipoChave informado
        fun para(key: String): DeletePixKeyRequest {
            reportMatcher(InstanceOf(DeletePixKeyRequest::class.java, "<any deletePixKeyRequest>"))
            return DeletePixKeyRequest(key)
        }

        // cópia de como o Mockito cria os métodos .any<OutraClasse)
        private fun reportMatcher(matcher: ArgumentMatcher<*>) {
            ThreadSafeMockingProgress.mockingProgress().argumentMatcherStorage.reportMatcher(matcher)
        }
    }

}