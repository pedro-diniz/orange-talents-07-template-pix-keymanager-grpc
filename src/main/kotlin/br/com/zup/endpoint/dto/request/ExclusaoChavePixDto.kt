package br.com.zup.endpoint.dto.request

import br.com.zup.utils.validation.UUIDValido
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

@Introspected
data class ExclusaoChavePixDto(
    @field:NotBlank @UUIDValido val clientId: String,
    @field:NotBlank val chavePix: String
)