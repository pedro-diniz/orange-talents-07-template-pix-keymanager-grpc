package br.com.zup.pix

import br.com.zup.TipoChave
import br.com.zup.TipoConta
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.validation.validator.Validator
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

@MicronautTest
internal class ChavePixValidatorTest(
    val validator: Validator
) {

    @Test
    internal fun `deve passar na validacao do cpf`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.CPF,
            "11122233344",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isEmpty())
    }

    @Test
    internal fun `nao deve passar na validacao do cpf`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.CPF,
            "123456789",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }

    @Test
    internal fun `nao deve passar na validacao com cpf nulo`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.CPF,
            "",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }

    @Test
    internal fun `deve passar na validacao do email`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.EMAIL,
            "fulano@zup.com.br",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isEmpty())
    }

    @Test
    internal fun `nao deve passar na validacao do email`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.EMAIL,
            "fulano#zup.com.br",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }

    @Test
    internal fun `nao deve passar na validacao com email nulo`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.EMAIL,
            "",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }

    @Test
    internal fun `deve passar na validacao do telefone celular`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.TELEFONE_CELULAR,
            "+5584996327131",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isEmpty())
    }

    @Test
    internal fun `nao deve passar na validacao do telefone celular`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.TELEFONE_CELULAR,
            "+55(84)99632-7131",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }

    @Test
    internal fun `nao deve passar na validacao com telefone celular nulo`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.TELEFONE_CELULAR,
            "",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }

    @Test
    internal fun `deve passar na validacao da chave aleatoria`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.CHAVE_ALEATORIA,
            "",
            TipoConta.CONTA_POUPANCA
        )

        println(novaChavePixDto.chavePix)

        val setErros = validator.validate(novaChavePixDto)
        val chavePix = novaChavePixDto.toModel()

        assertTrue(setErros.isEmpty())
        assertTrue(chavePix.chavePix.isNotBlank())

    }

    @Test
    internal fun `nao deve passar na validacao da chave aleatoria`() {

        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.CHAVE_ALEATORIA,
            "eu deveria ser vazia",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }

    @Test
    internal fun `nao deve passar com tipo chave unknown`() {
        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.TIPO_CHAVE_UNKNOWN,
            "eu deveria ser vazia",
            TipoConta.CONTA_POUPANCA
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }

    @Test
    internal fun `nao deve passar com tipo conta unknown`() {
        val novaChavePixDto = NovaChavePixDto(
            "0d1bb194-3c52-4e67-8c35-a93c0af1111z",
            TipoChave.CPF,
            "12345678909",
            TipoConta.TIPO_CONTA_UNKNOWN
        )

        val setErros = validator.validate(novaChavePixDto)
        assertTrue(setErros.isNotEmpty())
    }
}