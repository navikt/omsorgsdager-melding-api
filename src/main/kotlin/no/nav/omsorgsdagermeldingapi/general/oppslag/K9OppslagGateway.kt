package no.nav.omsorgsdagermeldingapi.general.oppslag

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.ApiGatewayApiKey
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import java.net.URI

abstract class K9OppslagGateway(
    protected val baseUrl: URI,
    private val apiGatewayApiKey: ApiGatewayApiKey
): HealthCheck {

    protected fun generateHttpRequest(
        idToken: IdToken,
        url: String,
        callId: CallId
    ): Request {
        return url
            .httpGet()
            .header(
                HttpHeaders.Authorization to "Bearer ${idToken.value}",
                HttpHeaders.Accept to "application/json",
                HttpHeaders.XCorrelationId to callId.value,
                apiGatewayApiKey.headerKey to apiGatewayApiKey.value
            )
    }
}