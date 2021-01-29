package no.nav.omsorgsdagermeldingapi.mellomlagring

import com.github.fppt.jedismock.RedisServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.util.*
import no.nav.omsorgsdagermeldingapi.Configuration
import no.nav.omsorgsdagermeldingapi.TestConfiguration
import no.nav.omsorgsdagermeldingapi.redis.RedisConfig
import no.nav.omsorgsdagermeldingapi.redis.RedisConfigurationProperties
import no.nav.omsorgsdagermeldingapi.redis.RedisMockUtil
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

        val redisClient = RedisConfig(RedisConfigurationProperties(true)).redisClient(
            Configuration(
                HoconApplicationConfig(ConfigFactory.parseMap(TestConfiguration.asMap(redisServer = redisServer)))
            )
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
