package br.com.zup.endpoint.dto.request

import br.com.zup.utils.validation.UUIDValido
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

@Introspected
data class ListagemChavesPixDto(@field:NotBlank @UUIDValido val clientId: String)