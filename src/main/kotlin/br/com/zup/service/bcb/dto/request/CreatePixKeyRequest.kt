package br.com.zup.service.bcb.dto.request

import br.com.zup.TipoChave
import br.com.zup.TipoConta
import br.com.zup.service.bcb.dto.KeyType
import javax.persistence.EnumType
import javax.persistence.Enumerated

class CreatePixKeyRequest(tipoChave: TipoChave, chave: String?, tipoConta: TipoConta) {

    @Enumerated(EnumType.STRING)
    val keyType = converteKeyType(tipoChave)

    var key : String? = chave
        private set
    val bankAccount = gerarConta(tipoConta)
    val owner = OwnerRequest(tipoChave, chave)

    fun converteKeyType(tipoChave: TipoChave) : KeyType {

        when(tipoChave) {
            TipoChave.CPF -> return KeyType.CPF
            TipoChave.TELEFONE_CELULAR -> return KeyType.PHONE
            TipoChave.EMAIL -> return KeyType.EMAIL
            TipoChave.CHAVE_ALEATORIA -> {
                limpaChaveAleatoria()
                return KeyType.RANDOM
            }
            else -> throw IllegalStateException("tipo de chave não permitido foi recebido")
        }

    }

    fun gerarConta(tipoConta: TipoConta) : BankAccountRequest {
        return BankAccountRequest(tipoConta = tipoConta)
    }

    private fun limpaChaveAleatoria() {
        key = null
    }

    override fun toString(): String {
        return "CreatePixKeyRequest(keyType=$keyType, key=$key, bankAccount=$bankAccount, owner=$owner)"
    }


}