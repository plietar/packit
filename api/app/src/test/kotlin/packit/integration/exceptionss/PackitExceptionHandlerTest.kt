package packit.integration.exceptionss

import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import packit.integration.IntegrationTest
import packit.integration.WithAuthenticatedUser
import kotlin.test.assertEquals

@WithAuthenticatedUser
class PackitExceptionHandlerTest : IntegrationTest()
{
    @Test
    fun `throws exception when client error occurred`()
    {
        val result: ResponseEntity<String> = restTemplate.exchange(
            "/packets/metadata/nonsense",
            HttpMethod.GET,
            getTokenizedHttpEntity()
        )

        assertEquals(result.statusCode, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}