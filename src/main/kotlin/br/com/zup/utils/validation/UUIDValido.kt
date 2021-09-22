package br.com.zup.utils.validation

import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.validation.validator.constraints.ConstraintValidator
import io.micronaut.validation.validator.constraints.ConstraintValidatorContext
import jakarta.inject.Singleton
import java.util.*
import javax.validation.Constraint

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UUIDValidoValidator::class])
annotation class UUIDValido(
    val message: String = "UUID inválido"
)

@Singleton
class UUIDValidoValidator : ConstraintValidator<UUIDValido, String> {

    override fun isValid(
        value: String,
        annotationMetadata: AnnotationValue<UUIDValido>,
        context: ConstraintValidatorContext
    ): Boolean {

        try {
            val UUID = UUID.fromString(value)
            return true
        }
        catch (e: IllegalArgumentException) {
            return false
        }
    }
}
