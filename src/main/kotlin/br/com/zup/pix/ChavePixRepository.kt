package br.com.zup.pix

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface ChavePixRepository : JpaRepository<ChavePix, Long> {

    fun existsByChavePix(chavePix: String) : Boolean
    fun countByChavePix(chavePix: String) : Int

}