package br.com.zup.service.bcb.dto.request

import br.com.zup.TipoConta
import br.com.zup.service.bcb.dto.AccountType
import javax.persistence.EnumType
import javax.persistence.Enumerated

class BankAccountRequest(tipoConta: TipoConta) {
    val participant = "60701190"
    val branch = geraDigitos(4)
    val accountNumber = geraDigitos(6)

    @Enumerated(EnumType.STRING)
    val accountType = converteTipoConta(tipoConta)

    fun geraDigitos(numeroDeDigitos: Int) : String {
        val numero = (0.. ("9".repeat(numeroDeDigitos)).toInt() ).random()
        var numeroComDigitos = numero.toString()
        while (numeroComDigitos.length != numeroDeDigitos) {

            numeroComDigitos = "0" + numeroComDigitos

        }
        return numeroComDigitos
    }

    fun converteTipoConta(tipoConta: TipoConta): AccountType {
        when(tipoConta) {
            TipoConta.CONTA_CORRENTE -> return AccountType.CACC
            TipoConta.CONTA_POUPANCA -> return AccountType.SVGS
            else -> throw IllegalStateException("tipo de conta inválido foi recebido")
        }
    }

    override fun toString(): String {
        return "BankAccountRequest(participant='$participant', branch='$branch', accountNumber='$accountNumber', accountType=$accountType)"
    }


}