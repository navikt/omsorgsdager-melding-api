package no.nav.omsorgsdagermeldingapi.barn

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
import no.nav.omsorgsdagermeldingapi.felles.k9SelvbetjeningOppslagKonfigurert
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.ApiGatewayApiKey
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import no.nav.omsorgsdagermeldingapi.general.oppslag.K9OppslagGateway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate

class BarnGateway (
    baseUrl: URI,
    private val apiGatewayApiKey: ApiGatewayApiKey
    ) : K9OppslagGateway(baseUrl, apiGatewayApiKey) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger("nav.BarnGateway")
        private const val HENTE_BARN_OPERATION = "hente-barn"
        private val objectMapper = jacksonObjectMapper().k9SelvbetjeningOppslagKonfigurert()

        private val attributter = Pair("a", listOf("barn[].aktør_id",
            "barn[].fornavn",
            "barn[].mellomnavn",
            "barn[].etternavn",
            "barn[].fødselsdato",
            "barn[].identitetsnummer"
            )
        )

    }

    suspend fun hentBarn(
        idToken: IdToken,
        callId : CallId
    ) : List<BarnOppslagDTO> {
        val barnUrl = Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf("meg"),
            queryParameters = mapOf(
                attributter
            )
        ).toString()

        val httpRequest = generateHttpRequest(idToken, barnUrl, callId)

        val oppslagRespons = Retry.retry(
            operation = HENTE_BARN_OPERATION,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = Operation.monitored(
                app = "omsorgsdager-melding-api",
                operation = HENTE_BARN_OPERATION,
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> objectMapper.readValue<BarnOppslagResponse>(success)},
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av informasjon om søkers barn")
                }
            )
        }
        return oppslagRespons.barn
    }

    private data class BarnOppslagResponse(val barn: List<BarnOppslagDTO>)

    data class BarnOppslagDTO (
        val fødselsdato: LocalDate,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val aktør_id: String,
        val identitetsnummer: String? = null
    )

    override suspend fun check(): Result { //TODO Trekke ut til en felles, trenger vi dobbel helsesjekk?
        val httpRequest = Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf("/isalive")
        ).toString()
            .httpGet()
            .header(
                HttpHeaders.Accept to "text/plain",
                apiGatewayApiKey.headerKey to apiGatewayApiKey.value
            )

        val (request, _, result) = Operation.monitored(
            app = "omsorgsdager-melding-api",
            operation = BarnGateway.HENTE_BARN_OPERATION,
            resultResolver = { 200 == it.second.statusCode }
        ) { httpRequest.awaitStringResponseResult() }

        return result.fold(
            { success -> Healthy("k9-selvbetjent-oppslag", "Helsesjekk mot k9-selvbetjent-oppslag OK.") },
            { error ->
                logger.error("Feil ved helsesjekk mot k9-selvbetjent-oppslag", error)
                UnHealthy("k9-selvbetjent-oppslag", "Helsesjekk mot k9-selvbetjent-oppslag feiler")
            }
        )
    }
}