package br.com.zup.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class GetNameFromIspbKtTest {

    @Test
    internal fun `deve retornar um banco`() {
        assertEquals("ITAÚ UNIBANCO S.A.", getNameFromIspb("60701190"))
    }

    @Test
    internal fun `deve retornar instituicao nao encontrada`() {
        assertEquals("Instituição não encontrada", getNameFromIspb("99999999"))
    }
}