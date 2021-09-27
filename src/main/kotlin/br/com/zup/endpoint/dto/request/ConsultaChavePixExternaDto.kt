package br.com.zup.endpoint.dto.request

import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
data class ConsultaChavePixExternaDto(
    @field:NotBlank @field:Size(max=77) val chavePix: String
) : ConsultaChavePix