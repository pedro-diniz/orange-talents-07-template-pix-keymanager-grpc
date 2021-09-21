package br.com.zup.pix

import br.com.zup.TipoChave
import br.com.zup.TipoConta
import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.validation.validator.constraints.ConstraintValidator
import io.micronaut.validation.validator.constraints.ConstraintValidatorContext
import jakarta.inject.Singleton
import javax.validation.Constraint

@MustBeDocumented
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ChavePixValidator::class])
annotation class ChavePixValida(
    val message: String = "chave pix inválida"
)

@Singleton
class ChavePixValidator : ConstraintValidator<ChavePixValida, NovaChavePixDto> {

    override fun isValid(
        value: NovaChavePixDto,
        annotationMetadata: AnnotationValue<ChavePixValida>,
        context: ConstraintValidatorContext
    ): Boolean {

        if (value.tipoConta == TipoConta.TIPO_CONTA_UNKNOWN) {
            return false
        }

        if (value.tipoChave == TipoChave.TIPO_CHAVE_UNKNOWN) {
            return false
        }

        when(value.tipoChave) {
            TipoChave.CHAVE_ALEATORIA -> {
                context.messageTemplate("chave aleatória não é nula")
                return (value.chavePix.isNullOrBlank())
            }
            TipoChave.CPF -> {
                context.messageTemplate("cpf inválido")
                return validaCpf(value.chavePix!!)
            }
            TipoChave.TELEFONE_CELULAR -> {
                context.messageTemplate("telefone celular inválido")
                return validaTelefoneCelular(value.chavePix!!)
            }
            TipoChave.EMAIL -> {
                context.messageTemplate("e-mail inválido")
                return validaEmail(value.chavePix!!)
            }
            else -> return false
        }

    }

    fun validaCpf(chavePix: String): Boolean {
        if (chavePix.isNotBlank()) {
            return "^[0-9]{11}".toRegex().matches(chavePix)
        }
        return false
    }

    fun validaTelefoneCelular(chavePix: String): Boolean {
        if (chavePix.isNotBlank()) {
            return "^\\+[1-9][0-9]\\d{1,14}\$".toRegex().matches(chavePix)
        }
        return false
    }

    fun validaEmail(chavePix: String): Boolean {
        if (chavePix.isNotBlank()) {
            return "^([_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{1,6}))?$".toRegex().matches(chavePix)
        }
        return false
    }

}
