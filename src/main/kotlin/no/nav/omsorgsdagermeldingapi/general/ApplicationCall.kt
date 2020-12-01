package no.nav.omsorgsdagermeldingapi.general

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import no.nav.omsorgsdagermeldingapi.felles.Metadata

data class CallId(val value : String)

fun ApplicationCall.getCallId() : CallId {
    return CallId(callId!!)
}

fun ApplicationCall.metadata() = Metadata(
    version = 1, //Versjonering ved store endringer.
    correlationId = getCallId().value,
    requestId = response.getRequestId()
)

fun ApplicationResponse.getRequestId(): String {
    return headers[HttpHeaders.XRequestId] ?: throw IllegalStateException("Request Id ikke satt")
}