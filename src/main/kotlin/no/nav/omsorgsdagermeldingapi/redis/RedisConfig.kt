package no.nav.omsorgsdagermeldingapi.redis

import io.ktor.util.*
import io.lettuce.core.RedisClient
import no.nav.omsorgsdagermeldingapi.Configuration

class RedisConfig {

    @KtorExperimentalAPI
    fun redisClient(configuration: Configuration): RedisClient {
        return RedisClient.create("redis://${configuration.getRedisHost()}:${configuration.getRedisPort()}")
    }
}
