package no.nav.omsorgsdagermeldingapi.felles

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured

val objectMapper: ObjectMapper = jacksonObjectMapper().dusseldorfConfigured()
    .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
    .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)

fun Any.somJson() = objectMapper.writeValueAsString(this)