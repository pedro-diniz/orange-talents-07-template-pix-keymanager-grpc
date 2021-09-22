package br.com.zup.service.bcb.dto.response

import java.time.LocalDateTime

data class DeletePixKeyResponse(
    val key: String,
    val participant: String,
    val deletedAt: LocalDateTime
)