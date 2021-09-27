package br.com.zup.utils.extensions

import br.com.zup.ExclusaoChavePixRequest
import br.com.zup.endpoint.dto.request.ExclusaoChavePixDto
import io.micronaut.validation.validator.Validator
import javax.validation.ConstraintViolationException

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