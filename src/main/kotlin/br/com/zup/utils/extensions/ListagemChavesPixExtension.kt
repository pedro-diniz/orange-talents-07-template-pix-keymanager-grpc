package br.com.zup.utils.extensions

import br.com.zup.ListagemChavesPixRequest
import br.com.zup.ListagemChavesPixResponse
import br.com.zup.endpoint.dto.request.ListagemChavesPixDto
import br.com.zup.model.ChavePix
import io.micronaut.validation.validator.Validator
import javax.validation.ConstraintViolationException

fun ListagemChavesPixRequest.validate(validator: Validator) : ListagemChavesPixDto {

    val listagemChavesPix = ListagemChavesPixDto(clientId = clientId)

    validator.validate(listagemChavesPix).let {
        if (it.isNotEmpty()) {
            throw ConstraintViolationException(it)
        }
        return listagemChavesPix
    }

}

fun converteParaGrpc(listaChaves: List<ChavePix>) : ListagemChavesPixResponse {
    val listaChavesResponse = ListagemChavesPixResponse.newBuilder()

    for (chave in listaChaves) {
        val chaveGrpc = chave.toGrpcResponse()
        println(chaveGrpc.toString())
        listaChavesResponse.addListagemChavesPixResponse(chaveGrpc)
    }

    return listaChavesResponse.build()
}