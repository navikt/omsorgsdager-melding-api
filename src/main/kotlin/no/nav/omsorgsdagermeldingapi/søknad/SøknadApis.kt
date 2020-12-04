package no.nav.omsorgsdagermeldingapi.søknad

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.omsorgsdagermeldingapi.barn.BarnService
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_FORDELE
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_KORONAOVERFØRE
import no.nav.omsorgsdagermeldingapi.felles.MELDING_URL_OVERFØRE
import no.nav.omsorgsdagermeldingapi.felles.formaterStatuslogging
import no.nav.omsorgsdagermeldingapi.general.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.general.getCallId
import no.nav.omsorgsdagermeldingapi.general.metadata
import no.nav.omsorgsdagermeldingapi.søknad.melding.Melding
import no.nav.omsorgsdagermeldingapi.søknad.melding.valider
import no.nav.omsorgsdagermeldingapi.vedlegg.DokumentEier
import no.nav.omsorgsdagermeldingapi.vedlegg.VedleggService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.soknadApis")

@KtorExperimentalLocationsAPI
fun Route.søknadApis(
    søknadService: SøknadService,
    idTokenProvider: IdTokenProvider,
    barnService: BarnService,
    vedleggService: VedleggService
) {

    @Location(MELDING_URL_KORONAOVERFØRE)
    class sendSøknadForOverføring
    post { _ : sendSøknadForOverføring ->
        logger.info("Mottatt ny melding om koronaoverføring av omsorgsdager.")

        logger.trace("Mapper melding")
        val melding = call.receive<Melding>()
        logger.trace("Melding mappet.")

        logger.trace("Oppdaterer barn med identitetsnummer")
        val listeOverBarnMedFnr = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
        melding.oppdaterBarnMedFnr(listeOverBarnMedFnr)
        logger.info("Oppdatering av identitetsnummer på barn OK")

        logger.trace("Validerer melding")
        melding.valider()
        logger.trace("Validering OK.")

        logger.info(formaterStatuslogging(melding.søknadId, "validert OK"))

        søknadService.registrer(
            melding = melding,
            metadata = call.metadata(),
            callId = call.getCallId(),
            idToken = idTokenProvider.getIdToken(call)
        )

        call.respond(HttpStatusCode.Accepted)
    }

    @Location(MELDING_URL_OVERFØRE)
    class sendSøknadForDeling
    post { _ : sendSøknadForDeling ->
        logger.info("Mottatt ny melding om overføring av omsorgsdager.")

        logger.trace("Mapper melding")
        val melding = call.receive<Melding>()
        logger.trace("Melding mappet.")

        logger.trace("Oppdaterer barn med identitetsnummer")
        val listeOverBarnMedFnr = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
        melding.oppdaterBarnMedFnr(listeOverBarnMedFnr)
        logger.info("Oppdatering av identitetsnummer på barn OK")

        logger.trace("Validerer melding")
        melding.valider()
        logger.trace("Validering OK.")

        logger.info(formaterStatuslogging(melding.søknadId, "validert OK"))

        søknadService.registrer(
            melding = melding,
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
            val melding = call.receive<Melding>()
            logger.trace("Melding mappet.")

            logger.trace("Oppdaterer barn med identitetsnummer")
            val listeOverBarnMedFnr = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
            melding.oppdaterBarnMedFnr(listeOverBarnMedFnr)
            logger.info("Oppdatering av identitetsnummer på barn OK")

            logger.trace("Validerer melding")
            melding.valider()
            logger.trace("Validering OK.")

            logger.info(formaterStatuslogging(melding.søknadId, "validert OK"))

            if(melding.fordeling == null) call.respond(HttpStatusCode.Forbidden) else {
                var eier = idTokenProvider.getIdToken(call).getSubject()!!
                logger.info("Persisterer samværsavtale")
                vedleggService.persisterVedlegg(
                    vedleggsUrls = melding.fordeling.samværsavtale,
                    callId = call.getCallId(),
                    eier = DokumentEier(eier)
                )
            }
            //TODO Vi må ha håndtere at ting kan feile når vi legger på kø. Hvis det skjer så må vedlegget som er persistert slettes
            søknadService.registrer(
                melding = melding,
                metadata = call.metadata(),
                callId = call.getCallId(),
                idToken = idTokenProvider.getIdToken(call)
            )

            call.respond(HttpStatusCode.Accepted)
    }

}