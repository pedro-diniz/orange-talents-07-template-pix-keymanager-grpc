package br.com.zup.utils.extensions

import br.com.zup.ChavePixRequest
import br.com.zup.ExclusaoChavePixRequest
import br.com.zup.endpoint.dto.request.ExclusaoChavePixDto
import br.com.zup.endpoint.dto.request.NovaChavePixDto
import io.micronaut.validation.validator.Validator
import javax.validation.ConstraintViolationException

fun ChavePixRequest.validate(validator: Validator): NovaChavePixDto {

    val novaChavePix = NovaChavePixDto(
        clientId = clientId,
        tipoChave = tipoChave,
        chavePix = chavePix,
        tipoConta = tipoConta
    )
    validator.validate(novaChavePix).let {
        if (it.isNotEmpty()) {
            throw ConstraintViolationException(it)
        }
        return novaChavePix
    }

}

fun ExclusaoChavePixRequest.validate(validator: Validator) : ExclusaoChavePixDto {

    val exclusaoChavePix = ExclusaoChavePixDto(
        clientId = clientId,
        chavePix = chavePix
    )
    validator.validate(exclusaoChavePix).let {
        if (it.isNotEmpty()) {
            throw ConstraintViolationException(it)
        }
        return exclusaoChavePix
    }

}

