package no.nav.omsorgsdagermeldingapi.mellomlagring

import com.github.fppt.jedismock.RedisServer

internal fun RedisServer.started() = apply { start() }
