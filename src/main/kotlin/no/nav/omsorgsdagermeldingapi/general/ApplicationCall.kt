package no.nav.omsorgsdagermeldingapi.general

import io.ktor.server.application.*
import io.ktor.server.plugins.callid.callId
import no.nav.omsorgsdagermeldingapi.felles.Metadata

data class CallId(val value : String)

fun ApplicationCall.getCallId() : CallId {
    return CallId(callId!!)
}

fun ApplicationCall.metadata() = Metadata(
    version = 1, //Versjonering ved store endringer.
    correlationId = getCallId().value
)
