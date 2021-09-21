package br.com.zup.extensions

import br.com.zup.ChavePixRequest
import br.com.zup.pix.NovaChavePixDto
import io.micronaut.validation.validator.Validator
import javax.validation.ConstraintViolationException
import javax.validation.Valid

fun ChavePixRequest.validate(validator: Validator): @Valid NovaChavePixDto {

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