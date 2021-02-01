package no.nav.omsorgsdagermeldingapi.mellomlagring

import com.github.fppt.jedismock.RedisServer
import io.ktor.util.*
import no.nav.omsorgsdagermeldingapi.redis.RedisConfig
import no.nav.omsorgsdagermeldingapi.redis.RedisStore
import org.junit.AfterClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@KtorExperimentalAPI
class MellomlagringTest {
    private companion object {
        val redisServer: RedisServer = RedisServer
            .newRedisServer(6379)
            .started()

        val redisClient = RedisConfig.redisClient(
            redisHost = redisServer.host,
            redisPort = redisServer.bindPort
        )

        val redisStore = RedisStore(
            redisClient
        )

        val mellomlagringService = MellomlagringService(
            redisStore,
            "VerySecretPass"
        )

        @AfterClass
        @JvmStatic
        fun teardown() {
            redisClient.shutdown()
            redisServer.stop()
        }
    }

    @Test
    internal fun `mellomlagre verdier`() {
        mellomlagringService.setMellomlagring("test", "test")

        val mellomlagring = mellomlagringService.getMellomlagring("test")

        assertEquals("test", mellomlagring)
    }

    @Test
    internal fun `verdier skal være krypterte`() {

        mellomlagringService.setMellomlagring("test", "test")

        val mellomlagring = mellomlagringService.getMellomlagring("test")
        assertNotNull(redisStore.get("mellomlagring_test"))
        assertNotEquals(mellomlagring, redisStore.get("test"))
    }

}