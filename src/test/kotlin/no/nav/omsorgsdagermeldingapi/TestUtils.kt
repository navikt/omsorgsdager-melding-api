package no.nav.helse

import com.github.tomakehurst.wiremock.http.Request
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.auth.IdToken
import no.nav.security.mock.oauth2.MockOAuth2Server

object TestUtils {

    fun getIdentFromIdToken(request: Request?): String {
        val idToken = IdToken(request!!.getHeader(HttpHeaders.Authorization).substringAfter("Bearer "))
        return idToken.getNorskIdentifikasjonsnummer()
    }

    fun MockOAuth2Server.issueToken(
        fnr: String,
        issuerId: String = "tokendings",
        audience: String = "dev-gcp:dusseldorf:omsorgsdager-melding-api",
        claims: Map<String, String> = mapOf("acr" to "Level4"),
        cookieName: String = "selvbetjening-idtoken",
        somCookie: Boolean = false,
    ): String {
        val jwtToken =
            issueToken(issuerId = issuerId, subject = fnr, audience = audience, claims = claims).serialize()
        return when (somCookie) {
            false -> jwtToken
            true -> "$cookieName=$jwtToken"
        }
    }
}