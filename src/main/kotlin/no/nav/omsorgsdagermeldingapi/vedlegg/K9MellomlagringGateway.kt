package no.nav.omsorgsdagermeldingapi.vedlegg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry.Companion.retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation.Companion.monitored
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.omsorgsdagermeldingapi.felles.k9MellomlagringKonfigurert
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Duration

class K9MellomlagringGateway(
    private val accessTokenClient: AccessTokenClient,
    private val lagreDokumentScopes: Set<String>,
    baseUrl : URI
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(K9MellomlagringGateway::class.java)
        private val objectMapper = jacksonObjectMapper().k9MellomlagringKonfigurert()
        private const val SLETTE_VEDLEGG_OPERATION = "slette-vedlegg"
        private const val HENTE_VEDLEGG_OPERATION = "hente-vedlegg"
        private const val LAGRE_VEDLEGG_OPERATION = "lagre-vedlegg"
    }

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private val url = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("v1", "dokument")
    )

    suspend fun lagreVedlegg(
        vedlegg: Vedlegg,
        idToken: IdToken,
        callId: CallId
    ): VedleggId {
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

                url
                    .toString()
                    .httpPost()
                    .body(contentStream)
                    .header(
                        HttpHeaders.Authorization to "Bearer ${idToken.value}",
                        HttpHeaders.ContentType to "application/json",
                        HttpHeaders.Accept to "application/json",
                        HttpHeaders.XCorrelationId to callId.value
                    )
                    .awaitStringResponseResult()
            }
            result.fold(
                { success -> VedleggId(objectMapper.readValue<CreatedResponseEntity>(success).id) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved lagring av vedlegg.")
                })
        }
    }

    suspend fun slettVedlegg(
        vedleggId: VedleggId,
        idToken: IdToken,
        callId: CallId,
        eier: DokumentEier
    ) : Boolean {
        val body = objectMapper.writeValueAsBytes(eier)

        val urlMedId = Url.buildURL(
            baseUrl = url,
            pathParts = listOf(vedleggId.value)
        )

        val httpRequest = urlMedId
            .toString()
            .httpDelete()
            .body(body)
            .header(
                HttpHeaders.Authorization to "Bearer ${idToken.value}",
                HttpHeaders.XCorrelationId to callId.value,
                HttpHeaders.ContentType to "application/json"
            )
        return requestSlettVedlegg(httpRequest)
    }

    private suspend fun requestSlettVedlegg(
        httpRequest: Request
    ) : Boolean = retry(
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
            { success ->
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

    suspend fun settPåHold(
        vedleggId: VedleggId,
        callId: CallId,
        eier: DokumentEier
    ) : Boolean {
        val authorizationHeader: String = cachedAccessTokenClient.getAccessToken(lagreDokumentScopes).asAuthoriationHeader()

        val urlMedId = Url.buildURL(
            baseUrl = url,
            pathParts = listOf(vedleggId.value, "persister")
        )

        val body =  objectMapper.writeValueAsBytes(eier)

        val httpRequest = urlMedId.toString()
            .httpPut()
            .body(body)
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.XCorrelationId to callId.value
            )

        return try { requestSlettVedlegg(httpRequest)}
        catch (cause: Throwable) {
            logger.error("Fikk ikke slettet vedlegg.")
            false
        }
    }

}

data class CreatedResponseEntity(val id : String)