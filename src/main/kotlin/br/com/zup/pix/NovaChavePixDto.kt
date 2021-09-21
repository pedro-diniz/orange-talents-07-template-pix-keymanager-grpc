package br.com.zup.pix

import br.com.zup.TipoChave
import br.com.zup.TipoConta
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Introspected @ChavePixValida
data class NovaChavePixDto(
    @field:NotBlank val clientId: String,
    @field:NotNull @Enumerated(EnumType.STRING) val tipoChave: TipoChave,
    var chavePix: String?,
    @field:NotNull @Enumerated(EnumType.STRING) val tipoConta: TipoConta
) {
    fun toModel(): ChavePix {
        verificaChaveAleatoria()
        return ChavePix(
            clientId, tipoChave, chavePix!!, tipoConta
        )
    }

    fun verificaChaveAleatoria() {
        if (tipoChave == TipoChave.CHAVE_ALEATORIA) {
            chavePix = UUID.randomUUID().toString()
        }
    }
}