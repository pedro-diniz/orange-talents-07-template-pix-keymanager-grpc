package br.com.zup.service.bcb.dto.request

import br.com.zup.TipoChave
import br.com.zup.service.bcb.dto.OwnerType
import javax.persistence.EnumType
import javax.persistence.Enumerated

class OwnerRequest(tipoChave: TipoChave, chave: String?) {

    @Enumerated(EnumType.STRING)
    val type = OwnerType.NATURAL_PERSON
    val name = gerarNome()
    var taxIdNumber = verificaCpf(tipoChave, chave)
        private set

    fun gerarNome() : String {

        val primeirosNomes = listOf("Alex", "Ana", "Yuri", "Rafael", "Pedro", "Amanda", "Anelise")
        val sobrenomes = listOf("Costa", "Dantas", "Castro", "Ponte", "Diniz", "Fonseca", "Barreto")

        val primeiroNomeIndex = (0..(primeirosNomes.size-1)).random()
        val sobrenomeIndex = (0..(sobrenomes.size-1)).random()

        return "${primeirosNomes[primeiroNomeIndex]} ${sobrenomes[sobrenomeIndex]}"

    }

    private fun verificaCpf(tipoChave: TipoChave, chave: String?) : String {
        if (tipoChave == TipoChave.CPF) {
            return chave!!
        }
        val numeroCpf = (0.. ("9".repeat(11)).toLong() ).random()
        var cpf = numeroCpf.toString()
        while (cpf.length != 11) {

            cpf = "0" + cpf

        }
        return cpf
    }

    override fun toString(): String {
        return "OwnerRequest(type=$type, name='$name', taxIdNumber='$taxIdNumber')"
    }


}