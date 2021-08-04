package no.nav.omsorgsdagermeldingapi.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import java.time.Duration

internal object RedisConfig {

    internal fun redisClient(redisHost: String, redisPort: Int): RedisClient {
        return RedisClient.create(RedisURI(redisHost, redisPort, Duration.ofSeconds(60)))
    }

}