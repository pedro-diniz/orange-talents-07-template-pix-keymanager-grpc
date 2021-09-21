package br.com.zup.pix

import br.com.zup.TipoChave
import br.com.zup.TipoConta
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
class ChavePix(
    @field:NotBlank val clientId: String,
    @field:NotNull @Enumerated(EnumType.STRING) val tipoChave: TipoChave,
    @field:Size(max=77) val chavePix: String,
    @field:NotNull @Enumerated(EnumType.STRING) val tipoConta: TipoConta
) {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

}