package packit.integration.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import packit.model.Packet
import packit.repository.PacketRepository
import java.time.Instant
import kotlin.test.assertEquals


class PacketRepositoryTest : RepositoryTest()
{
    @Autowired
    lateinit var packetRepository: PacketRepository


    val now = Instant.now().epochSecond

    val packet = listOf(
        Packet("20180818-164847-7574833b", "test1", "test name1", mapOf("name" to "value"), false, now),
        Packet("20170818-164847-7574853b", "test2", "test name2", mapOf("a" to 1), false, now + 1),
        Packet("20170819-164847-7574823b", "test3", "test name3", mapOf("alpha" to true), false, now + 3),
        Packet("20170819-164847-7574113a", "test4", "test name4", mapOf(), true, now + 4),
        Packet("20170819-164847-7574983b", "test4", "test name4", mapOf(), true, now + 2),
        Packet("20170819-164847-7574333b", "test1", "test name1", mapOf(), true, now + 5),

        )

    @BeforeEach
    fun setup()
    {
        packetRepository.deleteAll()
    }

    @Test
    fun `can get packets from db`()
    {
        packetRepository.saveAll(packet)

        val result = packetRepository.findAll()

        assertEquals(result, packet)
    }

    @Test
    fun `can get right order and data expected from findIdCountDataByName`()
    {
        packetRepository.saveAll(packet)

        val result = packetRepository.findPacketGroupSummaryByName("", PageRequest.of(0, 10)).map {
            object
            {
                val name = it.getName()
                val latestTime = it.getLatestTime()
                val latestId = it.getLatestId()
                val packetCount = it.getPacketCount()
            }
        }
        assertEquals(result.totalElements, 4)
        assertEquals(result.content[0].name, "test1")
        assertEquals(result.content[0].latestId, "20170819-164847-7574333b")
        assertEquals(result.content[0].latestTime, now + 5)
        assertEquals(result.content[0].packetCount, 2)
    }

    @Test
    fun `can filter correctly when calling findIdCountDataByName`()
    {
        packetRepository.saveAll(packet)

        val result = packetRepository.findPacketGroupSummaryByName("4", PageRequest.of(0, 10)).map {
            object
            {
                val name = it.getName()
                val latestTime = it.getLatestTime()
                val latestId = it.getLatestId()
                val packetCount = it.getPacketCount()
            }
        }

        assertEquals(result.totalElements, 1)
        assertEquals(result.content[0].name, "test4")
        assertEquals(result.content[0].latestId, "20170819-164847-7574113a")
        assertEquals(result.content[0].latestTime, now + 4)
        assertEquals(result.content[0].packetCount, 2)
    }

    @Test
    fun `can get sorted packet ids from db`()
    {
        packetRepository.saveAll(packet)

        val result = packetRepository.findAllIds()

        assertEquals(
            result,
            listOf(
                "20170818-164847-7574853b",
                "20170819-164847-7574113a",
                "20170819-164847-7574333b",
                "20170819-164847-7574823b",
                "20170819-164847-7574983b",
                "20180818-164847-7574833b"
            )
        )
    }

    @Test
    fun `most recent packet is null if no packets in db`()
    {
        val result = packetRepository.findTopByOrderByTimeDesc()
        assertEquals(result, null)
    }

    @Test
    fun `can get most recent packet from db`()
    {
        packetRepository.saveAll(packet)

        val result = packetRepository.findTopByOrderByTimeDesc()

        assertEquals(result!!.id, "20170819-164847-7574333b")
    }

    @Test
    fun `can get packet by id`()
    {
        packetRepository.saveAll(packet)

        val result = packetRepository.findById(packet[0].id)

        val id = result.orElseGet(null).id

        assertEquals(id, "20180818-164847-7574833b")
    }
}
