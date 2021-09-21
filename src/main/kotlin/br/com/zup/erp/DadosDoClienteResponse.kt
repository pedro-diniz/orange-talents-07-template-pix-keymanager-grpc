package br.com.zup.erp

import io.micronaut.core.annotation.Introspected

@Introspected
data class DadosDoClienteResponse(
    val id: String,
    val nome: String,
    val cpf: String,
    val instituicao: InstituicaoResponse
)