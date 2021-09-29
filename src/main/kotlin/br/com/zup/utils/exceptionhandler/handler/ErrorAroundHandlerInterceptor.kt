package br.com.zup.utils.exceptionhandler.handler

import br.com.zup.utils.exceptionhandler.exceptions.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.http.client.exceptions.HttpClientException
import jakarta.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
@InterceptorBean(ErrorAroundHandler::class)
class ErrorAroundHandlerInterceptor : MethodInterceptor<Any, Any> {

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {

        try {
            return context.proceed()
        }
        catch (ex: Exception) {

            val responseObserver = context.parameterValues[1] as StreamObserver<*>

            val status = when(ex) {
                is IllegalArgumentException -> Status.INVALID_ARGUMENT.withCause(ex).withDescription(ex.message)
                is ConstraintViolationException -> Status.INVALID_ARGUMENT.withCause(ex).withDescription("dados de entrada inválidos")
                is ChaveExistenteException -> Status.ALREADY_EXISTS.withCause(ex).withDescription(ex.message)
                is ChaveInexistenteException -> Status.NOT_FOUND.withCause(ex).withDescription(ex.message)
                is ClienteNaoEncontradoException -> Status.NOT_FOUND.withCause(ex).withDescription(ex.message)
                is ChaveDeOutroClienteException -> Status.PERMISSION_DENIED.withCause(ex).withDescription(ex.message)
                is DeleteProibidoException -> Status.PERMISSION_DENIED.withCause(ex).withDescription(ex.message)
                is HttpClientException -> Status.UNAVAILABLE.withCause(ex).withDescription(analisaSistemaOffline(ex))
                else -> Status.INTERNAL.withCause(ex).withDescription("Algo deu muito ruim")
            }

            responseObserver.onError(status.asRuntimeException())

        }

        return null

    }

    fun analisaSistemaOffline(ex: HttpClientException): String {
        var sistemaOffline = ""

        if (ex.message!!.contains(":8082")) {
            sistemaOffline = "Sistema do BCB encontra-se indisponível"
        }
        else if (ex.message!!.contains(":9091")) {
            sistemaOffline = "ERP do Itaú encontra-se indisponível"
        }

        return sistemaOffline

    }

}