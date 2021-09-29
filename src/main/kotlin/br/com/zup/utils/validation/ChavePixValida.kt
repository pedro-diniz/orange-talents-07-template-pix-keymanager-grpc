package br.com.zup.pix

import br.com.zup.TipoChave
import br.com.zup.TipoConta
import br.com.zup.endpoint.dto.request.NovaChavePixDto
import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.validation.validator.constraints.ConstraintValidator
import io.micronaut.validation.validator.constraints.ConstraintValidatorContext
import jakarta.inject.Singleton
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator
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
            context.messageTemplate("tipo de conta inválido")
            return false
        }

        when (value.tipoChave) {
            TipoChave.CHAVE_ALEATORIA -> {
                context.messageTemplate("chave aleatória não é nula")
                return (value.chavePix.isNullOrBlank())
            }
            TipoChave.CPF -> {
                context.messageTemplate("cpf inválido")
                return validaCpf(value.chavePix)
            }
            TipoChave.TELEFONE_CELULAR -> {
                context.messageTemplate("telefone celular inválido")
                return validaTelefoneCelular(value.chavePix)
            }
            TipoChave.EMAIL -> {
                context.messageTemplate("e-mail inválido")
                return validaEmail(value.chavePix)
            }
            else -> return false
        }

    }

    fun validaCpf(chavePix: String?): Boolean {
        if (chavePix.isNullOrBlank()) {
            return false
        }
        return CPFValidator().run {
            initialize(null)
            isValid(chavePix, null)
        }
    }

    fun validaTelefoneCelular(chavePix: String?): Boolean {
        if (chavePix.isNullOrBlank()) {
            return false
        }
        return "^\\+[1-9][0-9]\\d{1,14}\$".toRegex().matches(chavePix)
    }

    fun validaEmail(chavePix: String?): Boolean {
        if (chavePix.isNullOrBlank()) {
            return false
        }
        return EmailValidator().run {
            initialize(null)
            isValid(chavePix, null)
        }
    }

}
