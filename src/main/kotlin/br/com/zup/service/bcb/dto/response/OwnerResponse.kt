package br.com.zup.service.bcb.dto.response

import br.com.zup.service.bcb.dto.OwnerType

data class OwnerResponse(
    val type: OwnerType,
    val name: String,
    val taxIdNumber: String
)