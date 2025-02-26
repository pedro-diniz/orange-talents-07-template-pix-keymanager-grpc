package br.com.zup.endpoint.dto.request

import br.com.zup.service.bcb.dto.request.DeletePixKeyRequest
import br.com.zup.utils.validation.UUIDValido
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
data class ExclusaoChavePixDto(
    @field:NotBlank @UUIDValido val clientId: String,
    @field:NotBlank @field:Size(max=77) val chavePix: String
) {

    fun toBcbRequest() : DeletePixKeyRequest {
        return DeletePixKeyRequest(chavePix)
    }

}