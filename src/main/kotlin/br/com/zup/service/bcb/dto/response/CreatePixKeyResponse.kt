package br.com.zup.service.bcb.dto.response

import br.com.zup.service.bcb.dto.KeyType
import java.time.LocalDateTime

data class CreatePixKeyResponse(
    val keyType: KeyType,
    val key: String,
    val bankAccount: BankAccountResponse,
    val owner: OwnerResponse,
    val createdAt: LocalDateTime
)