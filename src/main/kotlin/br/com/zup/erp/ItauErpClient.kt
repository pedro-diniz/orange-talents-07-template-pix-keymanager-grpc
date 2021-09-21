package br.com.zup.erp

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client

@Client("http://localhost:9091/api/v1/clientes")
interface ItauErpClient {

    @Get("/{clientId}")
    fun consulta(clientId: String) : HttpResponse<DadosDoClienteResponse>

}