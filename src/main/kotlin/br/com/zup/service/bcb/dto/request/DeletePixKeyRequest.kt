package br.com.zup.service.bcb.dto.request

import io.micronaut.core.annotation.Introspected

@Introspected
class DeletePixKeyRequest(val key: String) {

    val participant = "60701190"

}