package br.com.zup.extensions

import br.com.zup.ChavePixRequest
import br.com.zup.pix.NovaChavePixDto
import javax.validation.Valid

fun ChavePixRequest.toModel() : @Valid NovaChavePixDto {
    return NovaChavePixDto(
        clientId = clientId,
        tipoChave = tipoChave,
        chavePix = chavePix,
        tipoConta = tipoConta
    )
}