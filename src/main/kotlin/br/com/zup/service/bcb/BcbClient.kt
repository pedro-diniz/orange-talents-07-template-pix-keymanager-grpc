package br.com.zup.service.bcb

import br.com.zup.service.bcb.dto.request.CreatePixKeyRequest
import br.com.zup.service.bcb.dto.request.DeletePixKeyRequest
import br.com.zup.service.bcb.dto.response.CreatePixKeyResponse
import br.com.zup.service.bcb.dto.response.DeletePixKeyResponse
import br.com.zup.service.bcb.dto.response.PixKeyDetailResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("http://localhost:8082/api/v1/pix/keys")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
interface BcbClient {

    @Post("/")
    fun cadastraBcb(@Body keyRequest: CreatePixKeyRequest) : HttpResponse<CreatePixKeyResponse>

    @Delete("/{key}")
    fun apagaBcb(@PathVariable key: String, @Body keyRequest: DeletePixKeyRequest) : HttpResponse<DeletePixKeyResponse?>

    @Get("/{key}")
    fun consultaBcb(@PathVariable key: String) : HttpResponse<PixKeyDetailResponse?>

}