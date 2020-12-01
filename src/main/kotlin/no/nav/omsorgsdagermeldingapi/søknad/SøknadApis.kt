package no.nav.omsorgsdagermeldingapi.søknad

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_DELE
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_FORDELE
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_OVERFØRE
import no.nav.omsorgsdagermeldingapi.felles.formaterStatuslogging
import no.nav.omsorgsdagermeldingapi.general.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.general.getCallId
import no.nav.omsorgsdagermeldingapi.general.metadata
import no.nav.omsorgsdagermeldingapi.søknad.søknad.Søknad
import no.nav.omsorgsdagermeldingapi.søknad.søknad.valider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.soknadApis")

@KtorExperimentalLocationsAPI
fun Route.søknadApis(
    søknadService: SøknadService,
    idTokenProvider: IdTokenProvider
) {

    @Location(MELDING_URL_OVERFØRE)
    class sendSøknadForOverføring
    post { _ : sendSøknadForOverføring ->
        logger.info("Mottatt ny melding om overføring av omsorgsdager.")

        logger.trace("Mapper melding")
        val søknad = call.receive<Søknad>()
        logger.trace("Melding mappet.")

        logger.trace("Validerer melding")
        søknad.valider()
        logger.trace("Validering OK.")

        logger.info(formaterStatuslogging(søknad.søknadId, "validert OK"))

        søknadService.registrer(
            søknad = søknad,
            metadata = call.metadata(),
            callId = call.getCallId(),
            idToken = idTokenProvider.getIdToken(call)
        )

        call.respond(HttpStatusCode.Accepted)
    }

    @Location(MELDING_URL_DELE)
    class sendSøknadForDeling
    post { _ : sendSøknadForDeling ->
        logger.info("Mottatt ny melding om deling av omsorgsdager.")

        logger.trace("Mapper melding")
        val søknad = call.receive<Søknad>()
        logger.trace("Melding mappet.")

        logger.trace("Validerer melding")
        søknad.valider()
        logger.trace("Validering OK.")

        logger.info(formaterStatuslogging(søknad.søknadId, "validert OK"))

        søknadService.registrer(
            søknad = søknad,
            metadata = call.metadata(),
            callId = call.getCallId(),
            idToken = idTokenProvider.getIdToken(call)
        )

        call.respond(HttpStatusCode.Accepted)
    }


    @Location(MELDING_URL_FORDELE)
    class sendSøknadForFordeling
    post { _ : sendSøknadForFordeling ->
        logger.info("Mottatt ny melding om fordeling av omsorgsdager.")

        logger.trace("Mapper melding")
        val søknad = call.receive<Søknad>()
        logger.trace("Melding mappet.")

        logger.trace("Validerer melding")
        søknad.valider()
        logger.trace("Validering OK.")

        logger.info(formaterStatuslogging(søknad.søknadId, "validert OK"))

        søknadService.registrer(
            søknad = søknad,
            metadata = call.metadata(),
            callId = call.getCallId(),
            idToken = idTokenProvider.getIdToken(call)
        )

        call.respond(HttpStatusCode.Accepted)
    }

}