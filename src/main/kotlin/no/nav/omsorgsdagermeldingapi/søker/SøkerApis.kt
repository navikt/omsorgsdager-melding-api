package no.nav.omsorgsdagermeldingapi.søker

import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdagermeldingapi.felles.SØKER_URL
import no.nav.omsorgsdagermeldingapi.general.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.general.getCallId

@KtorExperimentalLocationsAPI
fun Route.søkerApis(
    søkerService: SøkerService,
    idTokenProvider: IdTokenProvider
) {

    @Location(SØKER_URL)
    class getSoker

    get { _: getSoker ->
        call.respond(
            søkerService.getSøker(
                idToken = idTokenProvider.getIdToken(call),
                callId = call.getCallId()
            )
        )
    }
}

