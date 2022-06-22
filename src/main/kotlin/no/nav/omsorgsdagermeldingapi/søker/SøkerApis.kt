package no.nav.omsorgsdagermeldingapi.søker

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.dusseldorf.ktor.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.felles.SØKER_URL
import no.nav.omsorgsdagermeldingapi.general.getCallId
import no.nav.omsorgsdagermeldingapi.general.oppslag.TilgangNektetException
import no.nav.omsorgsdagermeldingapi.general.oppslag.respondTilgangNektetProblemDetail
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("no.nav.omsorgsdagermeldingapi.søker.søkerApis")

fun Route.søkerApis(
    søkerService: SøkerService,
    idTokenProvider: IdTokenProvider
) {

    get(SØKER_URL) {
        try {
            call.respond(søkerService.getSøker(idTokenProvider.getIdToken(call), call.getCallId()))
        } catch (e: Exception) {
            when(e){
                is TilgangNektetException -> call.respondTilgangNektetProblemDetail(logger, e)
                else -> throw e
            }
        }
    }
}

