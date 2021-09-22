package br.com.zup.service.bcb.dto.response

import br.com.zup.service.bcb.dto.AccountType

data class BankAccountResponse(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountType
)