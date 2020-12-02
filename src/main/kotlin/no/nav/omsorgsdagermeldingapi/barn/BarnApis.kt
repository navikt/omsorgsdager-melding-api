package no.nav.omsorgsdagermeldingapi.barn

import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdagermeldingapi.felles.BARN_URL
import no.nav.omsorgsdagermeldingapi.general.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.general.getCallId

@KtorExperimentalLocationsAPI
fun Route.barnApis(
    barnService: BarnService,
    idTokenProvider: IdTokenProvider
) {

    @Location(BARN_URL)
    class getBarn

    get { _: getBarn ->
        call.respond(
            BarnResponse(
                barnService.hentNåværendeBarn(
                    idToken = idTokenProvider.getIdToken(call),
                    callId = call.getCallId()
                )
            )
        )
    }
}
