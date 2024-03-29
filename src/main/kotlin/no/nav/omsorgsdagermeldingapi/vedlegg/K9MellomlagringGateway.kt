package no.nav.omsorgsdagermeldingapi.vedlegg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import io.ktor.http.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.helse.dusseldorf.ktor.auth.IdToken
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry.Companion.retry
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.ktor.metrics.Operation.Companion.monitored
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgsdagermeldingapi.felles.k9MellomlagringKonfigurert
import no.nav.omsorgsdagermeldingapi.general.CallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Duration

class K9MellomlagringGateway(
    private val baseUrl: URI,
    private val k9MellomlagringScope: Set<String>,
    private val accessTokenClient: AccessTokenClient,
    private val exchangeTokenClient: CachedAccessTokenClient,
    private val k9MellomlagringTokenxAudience: Set<String>
) : HealthCheck {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(K9MellomlagringGateway::class.java)
        private val objectMapper = jacksonObjectMapper().k9MellomlagringKonfigurert()
        private const val SLETTE_VEDLEGG_OPERATION = "slette-vedlegg"
        private const val HENTE_VEDLEGG_OPERATION = "hente-vedlegg"
        private const val LAGRE_VEDLEGG_OPERATION = "lagre-vedlegg"
        private const val PERSISTER_VEDLEGG = "persister-vedlegg"
        private const val SLETT_PERSISTERT_VEDLEGG = "slett-persistert-vedlegg"
    }

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    override suspend fun check(): Result {
        return try {
            accessTokenClient.getAccessToken(k9MellomlagringScope)
            Healthy("K9MellomlagringGateway", "Henting av access token for å persistere vedlegg.")
        } catch (cause: Throwable) {
            logger.error("Feil ved henting av access token for å persistere vedlegg", cause)
            UnHealthy("K9MellomlagringGateway", "Henting av access token for å persistere vedlegg.")
        }
    }

    suspend fun lagreVedlegg(
        vedlegg: Vedlegg,
        idToken: IdToken,
        callId: CallId
    ): String {
        val exchangeToken = IdToken(exchangeTokenClient.getAccessToken(k9MellomlagringTokenxAudience, idToken.value).token)
        logger.info("Utvekslet token fra {} med token fra {}.", idToken.issuer(), exchangeToken.issuer())

        val body = objectMapper.writeValueAsBytes(vedlegg)
        return retry(
            operation = LAGRE_VEDLEGG_OPERATION,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = monitored(
                app = "omsorgsdager-melding-api",
                operation = LAGRE_VEDLEGG_OPERATION,
                resultResolver = { 201 == it.second.statusCode }
            ) {
                val contentStream = { ByteArrayInputStream(body) }

                baseUrl
                    .toString()
                    .httpPost()
                    .body(contentStream)
                    .header(
                        HttpHeaders.Authorization to "Bearer ${exchangeToken.value}",
                        HttpHeaders.ContentType to "application/json",
                        HttpHeaders.Accept to "application/json",
                        HttpHeaders.XCorrelationId to callId.value
                    )
                    .awaitStringResponseResult()
            }
            result.fold(
                { success -> objectMapper.readValue<CreatedResponseEntity>(success).id },
                { error ->
                    logger.error(
                        "Error response = '${
                            error.response.body().asString("text/plain")
                        }' fra '${request.url}'"
                    )
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved lagring av vedlegg.")
                })
        }
    }

    suspend fun slettVedlegg(
        vedleggId: String,
        idToken: IdToken,
        callId: CallId,
        eier: DokumentEier
    ): Boolean {
        val exchangeToken = IdToken(exchangeTokenClient.getAccessToken(k9MellomlagringTokenxAudience, idToken.value).token)
        logger.info("Utvekslet token fra {} med token fra {}.", idToken.issuer(), exchangeToken.issuer())

        val body = objectMapper.writeValueAsBytes(eier)

        val urlMedId = Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf(vedleggId)
        )

        val httpRequest = urlMedId
            .toString()
            .httpDelete()
            .body(body)
            .header(
                HttpHeaders.Authorization to "Bearer ${exchangeToken.value}",
                HttpHeaders.XCorrelationId to callId.value,
                HttpHeaders.ContentType to "application/json"
            )
        return requestSlettVedlegg(httpRequest)
    }

    private suspend fun requestSlettVedlegg(
        httpRequest: Request
    ): Boolean = retry(
        operation = SLETTE_VEDLEGG_OPERATION,
        initialDelay = Duration.ofMillis(200),
        factor = 2.0,
        logger = logger
    ) {
        val (request, _, result) = monitored(
            app = "omsorgsdager-melding-api",
            operation = SLETTE_VEDLEGG_OPERATION,
            resultResolver = { 204 == it.second.statusCode }
        ) { httpRequest.awaitStringResponseResult() }

        result.fold(
            { _ ->
                logger.info("Suksess ved sletting av vedlegg")
                true
            },
            { error ->
                logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.error(error.toString())
                throw IllegalStateException("Feil ved sletting av vedlegg.")
            }
        )
    }

    private suspend fun requestHentVedlegg(
        httpRequest: Request
    ): Vedlegg? = retry(
        operation = HENTE_VEDLEGG_OPERATION,
        initialDelay = Duration.ofMillis(200),
        factor = 2.0,
        logger = logger
    ) {
        val (request, _, result) = monitored(
            app = "omsorgsdager-melding-api",
            operation = HENTE_VEDLEGG_OPERATION,
            resultResolver = { 200 == it.second.statusCode }
        ) { httpRequest.awaitStringResponseResult() }

        result.fold(
            { success ->
                logger.info("Suksess ved henting av vedlegg")
                ResolvedVedlegg(objectMapper.readValue<Vedlegg>(success))
            },
            { error ->
                logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.error(error.toString())
                throw IllegalStateException("Feil ved henting av vedlegg.")
            }
        ).vedlegg
    }

    internal suspend fun slettPersistertVedlegg(
        vedleggId: List<String>,
        callId: CallId,
        eier: DokumentEier
    ) {
        val authorizationHeader: String =
            cachedAccessTokenClient.getAccessToken(k9MellomlagringScope).asAuthoriationHeader()

        coroutineScope {
            val deferred = mutableListOf<Deferred<Unit>>()
            vedleggId.forEach {
                deferred.add(async {
                    requestSlettPersisterVedlegg(
                        vedleggId = it,
                        callId = callId,
                        eier = eier,
                        authorizationHeader = authorizationHeader
                    )
                })
            }
            deferred.awaitAll()
        }
    }

    private suspend fun requestSlettPersisterVedlegg(
        vedleggId: String,
        callId: CallId,
        eier: DokumentEier,
        authorizationHeader: String
    ) {

        val urlMedId = Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf(vedleggId)
        )

        val body = objectMapper.writeValueAsBytes(eier)

        val httpRequest = urlMedId.toString()
            .httpDelete()
            .body(body)
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.XCorrelationId to callId.value,
                HttpHeaders.ContentType to "application/json"
            )

        val (request, _, result) = Operation.monitored(
            app = "omsorgsdager-melding-api",
            operation = SLETT_PERSISTERT_VEDLEGG,
            resultResolver = { 204 == it.second.statusCode }
        ) {
            httpRequest.awaitStringResponseResult()
        }


        result.fold(
            { _ -> logger.info("Vellykket sletting av persistert vedlegg") },
            { error ->
                logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.error("Feil ved sletting av persistert vedlegg. $error")
                throw IllegalStateException("Feil ved sletting av persistert vedlegg.")
            }
        )
    }

    internal suspend fun persisterVedlegger(
        vedleggId: List<String>,
        callId: CallId,
        eier: DokumentEier
    ) {
        val authorizationHeader: String =
            cachedAccessTokenClient.getAccessToken(k9MellomlagringScope).asAuthoriationHeader()

        coroutineScope {
            val deferred = mutableListOf<Deferred<Unit>>()
            vedleggId.forEach {
                deferred.add(async {
                    requestPersisterVedlegg(
                        vedleggId = it,
                        callId = callId,
                        eier = eier,
                        authorizationHeader = authorizationHeader
                    )
                })
            }
            deferred.awaitAll()
        }
    }

    private suspend fun requestPersisterVedlegg(
        vedleggId: String,
        callId: CallId,
        eier: DokumentEier,
        authorizationHeader: String
    ) {

        val urlMedId = Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf(vedleggId, "persister")
        )

        val body = objectMapper.writeValueAsBytes(eier)

        val httpRequest = urlMedId.toString()
            .httpPut()
            .body(body)
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.XCorrelationId to callId.value,
                HttpHeaders.ContentType to "application/json"
            )

        val (request, _, result) = Operation.monitored(
            app = "omsorgsdager-melding-api",
            operation = PERSISTER_VEDLEGG,
            resultResolver = { 204 == it.second.statusCode }
        ) {
            httpRequest.awaitStringResponseResult()
        }


        result.fold(
            { _ -> logger.info("Vellykket persistering av vedlegg") },
            { error ->
                logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                logger.error("Feil ved persistering av vedlegg. $error")
                throw IllegalStateException("Feil ved persistering av vedlegg.")
            }
        )
    }

    suspend fun hentVedlegg(vedleggId: String, idToken: IdToken, eier: DokumentEier, callId: CallId): Vedlegg? {
        val exchangeToken = IdToken(exchangeTokenClient.getAccessToken(k9MellomlagringTokenxAudience, idToken.value).token)
        logger.info("Utvekslet token fra {} med token fra {}.", idToken.issuer(), exchangeToken.issuer())

        val body = objectMapper.writeValueAsBytes(eier)

        val urlMedId = Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf(vedleggId)
        )

        val httpRequest = urlMedId
            .toString()
            .httpPost()
            .body(body)
            .header(
                HttpHeaders.Authorization to "Bearer ${exchangeToken.value}",
                HttpHeaders.XCorrelationId to callId.value,
                HttpHeaders.ContentType to "application/json",
                HttpHeaders.Accept to "application/json"
            )
        return requestHentVedlegg(httpRequest)
    }

}

data class CreatedResponseEntity(val id: String)
private data class ResolvedVedlegg(val vedlegg: Vedlegg? = null)
