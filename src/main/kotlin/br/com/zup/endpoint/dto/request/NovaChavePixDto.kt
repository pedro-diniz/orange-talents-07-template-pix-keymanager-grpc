package br.com.zup.endpoint.dto.request

import br.com.zup.TipoChave
import br.com.zup.TipoConta
import br.com.zup.model.ChavePix
import br.com.zup.pix.ChavePixValida
import br.com.zup.service.bcb.dto.request.CreatePixKeyRequest
import br.com.zup.utils.validation.UUIDValido
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Introspected @ChavePixValida
data class NovaChavePixDto(
    @field:NotBlank @UUIDValido val clientId: String,
    @field:NotNull @Enumerated(EnumType.STRING) val tipoChave: TipoChave,
    var chavePix: String? = null,
    @field:NotNull @Enumerated(EnumType.STRING) val tipoConta: TipoConta
) {
    fun toModel(): ChavePix {

        return ChavePix(
            clientId, tipoChave, chavePix!!, tipoConta
        )
    }

    fun toBcbRequest() : CreatePixKeyRequest {
        return CreatePixKeyRequest(
            tipoChave = tipoChave,
            chave = chavePix!!,
            tipoConta = tipoConta,
        )
    }

}