package no.nav.omsorgsdagermeldingapi.redis

import no.nav.omsorgsdagermeldingapi.redis.RedisMockUtil.startRedisMocked

class RedisConfigurationProperties(private val redisMocked: Boolean) {

    fun startInMemoryRedisIfMocked() {
        if (redisMocked) {
            startRedisMocked()
        }
    }
}