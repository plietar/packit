package packit.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.servlet.NoHandlerFoundException
import packit.model.ErrorDetail
import packit.service.OutpackServerException
import java.util.*

@ControllerAdvice
class PackitExceptionHandler
{
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(e: Exception): Any
    {
        return ErrorDetail(HttpStatus.NOT_FOUND, e.message ?: "")
            .toResponseEntity()
    }

    @ExceptionHandler(HttpClientErrorException::class, HttpServerErrorException::class)
    fun handleHttpClientErrorException(e: Exception): ResponseEntity<String>
    {
        return ErrorDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.message ?: "")
            .toResponseEntity()
    }

    @ExceptionHandler(OutpackServerException::class)
    fun handleOutpackServerException(e: OutpackServerException): ResponseEntity<String>
    {
        val clientError = e.cause!! as HttpStatusCodeException
        val message = clientError.responseBodyAsString
        return ResponseEntity<String>(message, clientError.responseHeaders, clientError.statusCode)
    }

    @ExceptionHandler(PackitException::class)
    fun handlePackitException(
        error: PackitException
    ): ResponseEntity<String>
    {
        val resourceBundle = getBundle()

        return ErrorDetail(error.httpStatus, resourceBundle.getString(error.key))
            .toResponseEntity()
    }

    private fun getBundle(): ResourceBundle
    {
        return ResourceBundle.getBundle("errorBundle", Locale.ENGLISH)
    }
}