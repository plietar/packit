package packit.service

import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import packit.exceptions.PackitException
import packit.model.Packet
import packit.model.PacketMetadata
import packit.repository.PacketRepository
import java.security.MessageDigest
import java.time.Instant

interface PacketService
{
    fun getPackets(): List<Packet>
    fun getChecksum(): String
    fun importPackets()
    fun getMetadataBy(id: String): PacketMetadata
    fun getFileByHash(hash: String, inline: Boolean, filename: String): Pair<InputStreamResource, HttpHeaders>
}

@Service
class BasePacketService(
        private val packetRepository: PacketRepository,
        private val outpackServerClient: OutpackServer
) : PacketService
{
    override fun importPackets()
    {
        val mostRecent = packetRepository.findTopByOrderByTimeDesc()?.time
        val now = Instant.now().epochSecond
        val packets = outpackServerClient.getMetadata(mostRecent)
                .map {
                    Packet(
                            it.id, it.name, it.name,
                            it.parameters ?: mapOf(), false, now
                    )
                }
        packetRepository.saveAll(packets)
    }

    override fun getPackets(): List<Packet>
    {
        return packetRepository.findAll()
    }

    override fun getChecksum(): String
    {
        return packetRepository.findAllIds()
                .joinToString("")
                .toSHA256()
    }

    private fun String.toSHA256(): String
    {
        return "sha256:${
            MessageDigest
                    .getInstance("SHA-256")
                    .digest(this.toByteArray()).toHex()
        }"
    }

    private fun ByteArray.toHex(): String
    {
        return this.joinToString("") { "%02x".format(it) }
    }

    override fun getMetadataBy(id: String): PacketMetadata
    {
        return outpackServerClient.getMetadataById(id)
            ?: throw PackitException("doesNotExist", HttpStatus.NOT_FOUND)
    }

    override fun getFileByHash(hash: String, inline: Boolean, filename: String): Pair<InputStreamResource, HttpHeaders>
    {
        val response = outpackServerClient.getFileByHash(hash)

        if (response?.first == null || response.first.isEmpty())
        {
            throw PackitException("doesNotExist", HttpStatus.NOT_FOUND)
        }

        val inputStream = response.first.toString().byteInputStream()

        val inputStreamResource = InputStreamResource(inputStream)

        val disposition = if (inline) "inline" else "attachment"

        val headers = HttpHeaders().apply {
            contentType = if (inline) MediaType.TEXT_HTML else response.second.contentType
            contentDisposition = ContentDisposition.parse("$disposition; filename='$filename'")
        }

        return inputStreamResource to headers
    }
}
