package no.nav.omsorgsdagermeldingapi.søker

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import no.nav.omsorgsdagermeldingapi.general.oppslag.K9OppslagGateway
import no.nav.omsorgsdagermeldingapi.general.oppslag.throwable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate

class SøkerGateway (
    baseUrl: URI
) : K9OppslagGateway(baseUrl) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger("nav.SokerGateway")
        private const val HENTE_SOKER_OPERATION = "hente-soker"
        private val objectMapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            registerModule(JavaTimeModule())
        }
        private val attributter = Pair("a", listOf("aktør_id", "fornavn", "mellomnavn", "etternavn", "fødselsdato"))
    }

    suspend fun hentSøker(
        idToken: IdToken,
        callId : CallId
    ) : SokerOppslagRespons {
        val sokerUrl = Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf("meg"),
            queryParameters = mapOf(
                attributter
            )
        ).toString()
        val httpRequest = generateHttpRequest(idToken, sokerUrl, callId)

        val oppslagRespons = Retry.retry(
            operation = HENTE_SOKER_OPERATION,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = Operation.monitored(
                app = "omsorgsdager-melding-api",
                operation = HENTE_SOKER_OPERATION,
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> objectMapper.readValue<SokerOppslagRespons>(success)},
                { error -> throw error.throwable(request, logger, "Feil ved henting av søkers personinformasjon")}
            )
        }
        return oppslagRespons
    }

    data class SokerOppslagRespons(
        val aktør_id: String,
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val fødselsdato: LocalDate
    )

    override suspend fun check(): Result {
        val httpRequest = Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf("/isalive")
        ).toString()
            .httpGet()
            .header(
                HttpHeaders.Accept to "text/plain"
            )

        val (_, _, result) = Operation.monitored(
            app = "omsorgsdager-melding-api",
            operation = HENTE_SOKER_OPERATION,
            resultResolver = { 200 == it.second.statusCode }
        ) { httpRequest.awaitStringResponseResult() }

        return result.fold(
            { _ -> Healthy("k9-selvbetjent-oppslag", "Helsesjekk mot k9-selvbetjent-oppslag OK.")},
            { error ->
                logger.error("Feil ved helsesjekk mot k9-selvbetjent-oppslag", error)
                UnHealthy("k9-selvbetjent-oppslag", "Helsesjekk mot k9-selvbetjent-oppslag feiler")
            }
        )
    }

}
