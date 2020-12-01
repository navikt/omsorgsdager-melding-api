package no.nav.omsorgsdagermeldingapi.general.auth

import io.ktor.application.*

class IdTokenProvider(
    private val cookieName : String
) {
    fun getIdToken(call: ApplicationCall) : IdToken {
        val cookie = call.request.cookies[cookieName] ?: throw CookieNotSetException(cookieName)
        return IdToken(value = cookie)
    }
}