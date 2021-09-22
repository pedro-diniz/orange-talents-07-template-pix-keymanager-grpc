package br.com.zup.service.erp

import io.micronaut.core.annotation.Introspected

@Introspected
data class InstituicaoResponse(val nome: String, val ispb: String)