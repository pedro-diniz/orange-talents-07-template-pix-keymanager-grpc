package br.com.zup.utils.extensions

import br.com.zup.ConsultaChavePixRequest
import br.com.zup.endpoint.dto.request.ConsultaChavePix
import br.com.zup.endpoint.dto.request.ConsultaChavePixExternaDto
import br.com.zup.endpoint.dto.request.ConsultaChavePixInternaDto
import io.micronaut.validation.validator.Validator
import javax.validation.ConstraintViolationException

fun ConsultaChavePixRequest.validate(validator: Validator) : ConsultaChavePix {

    lateinit var consultaChavePix : ConsultaChavePix

    // não existe nulo no Protobuf. O "null" do tipo Long é o número 0L.
    if (pixId != 0L && clientId.isNotBlank()) {
        if (chavePix.isBlank()) {
            consultaChavePix = ConsultaChavePixInternaDto(
                pixId = pixId,
                clientId = clientId
            )
        }
        else {
            consultaChavePix = ConsultaChavePixInternaDto(
                pixId = pixId,
                clientId = clientId,
                chavePix = chavePix
            )
        }
    }

    else if (chavePix.isNotBlank()) {
        consultaChavePix = ConsultaChavePixExternaDto(chavePix = chavePix)
    }

    else {
        throw IllegalArgumentException("dados de entrada não se encaixem em nenhuma modalidade de consulta")
    }

    validator.validate(consultaChavePix).let {
        if (it.isNotEmpty()) {
            throw ConstraintViolationException(it)
        }
        return consultaChavePix
    }
}