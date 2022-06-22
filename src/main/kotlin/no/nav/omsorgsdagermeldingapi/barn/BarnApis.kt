package no.nav.omsorgsdagermeldingapi.barn

import io.ktor.server.routing.Route
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.dusseldorf.ktor.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.felles.BARN_URL
import no.nav.omsorgsdagermeldingapi.general.getCallId
import no.nav.omsorgsdagermeldingapi.general.oppslag.TilgangNektetException
import no.nav.omsorgsdagermeldingapi.general.oppslag.respondTilgangNektetProblemDetail
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.omsorgsdagermeldingapi.barn.barnApis")

fun Route.barnApis(
    barnService: BarnService,
    idTokenProvider: IdTokenProvider
) {
    get(BARN_URL) {
        try {
            call.respond(
                BarnResponse(barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId()))
            )
        } catch (e: Exception) {
            when(e){
                is TilgangNektetException -> call.respondTilgangNektetProblemDetail(logger, e)
                else -> throw e
            }
        }
    }
}
