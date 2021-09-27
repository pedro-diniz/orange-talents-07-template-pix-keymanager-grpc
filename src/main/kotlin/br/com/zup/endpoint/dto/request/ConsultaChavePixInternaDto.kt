package br.com.zup.endpoint.dto.request

import br.com.zup.utils.validation.UUIDValido
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive
import javax.validation.constraints.Size

@Introspected
data class ConsultaChavePixInternaDto(
    @field:NotNull @field:Positive val pixId: Long,
    @field:NotBlank @UUIDValido val clientId: String,
    @field:Size(max=77) val chavePix: String? = null
) : ConsultaChavePix