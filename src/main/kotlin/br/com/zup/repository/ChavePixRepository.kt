package br.com.zup.repository

import br.com.zup.model.ChavePix
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository : JpaRepository<ChavePix, Long> {

    fun existsByChavePix(chavePix: String) : Boolean
    fun findByChavePix(chavePix: String) : Optional<ChavePix>
    fun findByClientId(clientId: String) : List<ChavePix>

}