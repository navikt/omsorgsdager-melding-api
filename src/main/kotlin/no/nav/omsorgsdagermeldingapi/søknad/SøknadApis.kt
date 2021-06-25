package no.nav.omsorgsdagermeldingapi.søknad

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.core.DefaultProblemDetails
import no.nav.helse.dusseldorf.ktor.core.respondProblemDetails
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_FORDELE
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_KORONAOVERFØRE
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_OVERFØRE
import no.nav.omsorgsdagermeldingapi.felles.formaterStatuslogging
import no.nav.omsorgsdagermeldingapi.general.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.general.getCallId
import no.nav.omsorgsdagermeldingapi.general.metadata
import no.nav.omsorgsdagermeldingapi.søknad.melding.Melding
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.soknadApis")

private val ikkeFordelingsmelding = DefaultProblemDetails(
    title = "feil-melding-på-endepunkt.",
    status = 400,
    detail = "Dette endepunktet krever en fordelingsmelding"
)

private val ikkeKoronaOverføringsmelding = DefaultProblemDetails(
    title = "feil-melding-på-endepunkt.",
    status = 400,
    detail = "Dette endepunktet krever en koronaoverføringsmelding"
)

private val ikkeOverføringmelding = DefaultProblemDetails(
    title = "feil-melding-på-endepunkt.",
    status = 400,
    detail = "Dette endepunktet krever en overføringsmelding"
)

fun Route.søknadApis(
    søknadService: SøknadService,
    idTokenProvider: IdTokenProvider
) {

    post(MELDING_URL_KORONAOVERFØRE) {
        val melding = call.receive<Melding>()
        logger.info(formaterStatuslogging(melding.søknadId, "mottatt om koronaoverføring av omsorgsdager."))

        when (melding.korona) {
            null -> call.respondProblemDetails(ikkeKoronaOverføringsmelding)
            else -> søknadService.registrer(
                melding = melding,
                metadata = call.metadata(),
                callId = call.getCallId(),
                idToken = idTokenProvider.getIdToken(call)
            )
        }

        call.respond(HttpStatusCode.Accepted)
    }

    post(MELDING_URL_OVERFØRE) {
        val melding = call.receive<Melding>()
        logger.info(formaterStatuslogging(melding.søknadId, "mottatt om overføring av omsorgsdager."))

        when (melding.overføring) {
            null -> call.respondProblemDetails(ikkeOverføringmelding)
            else -> søknadService.registrer(
                melding = melding,
                metadata = call.metadata(),
                callId = call.getCallId(),
                idToken = idTokenProvider.getIdToken(call)
            )

        }
        call.respond(HttpStatusCode.Accepted)
    }

    post(MELDING_URL_FORDELE) {
        val melding = call.receive<Melding>()
        logger.info(formaterStatuslogging(melding.søknadId, "mottatt om fordeling av omsorgsdager."))

        when (melding.fordeling) {
            null -> call.respondProblemDetails(ikkeFordelingsmelding)
            else -> søknadService.registrer(
                melding = melding,
                metadata = call.metadata(),
                callId = call.getCallId(),
                idToken = idTokenProvider.getIdToken(call)
            )
        }
        call.respond(HttpStatusCode.Accepted)
    }
}