package no.nav.omsorgsdagermeldingapi.søknad

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.core.DefaultProblemDetails
import no.nav.helse.dusseldorf.ktor.core.respondProblemDetails
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
import no.nav.omsorgsdagermeldingapi.søknad.melding.validerVedlegg
import no.nav.omsorgsdagermeldingapi.vedlegg.DokumentEier
import no.nav.omsorgsdagermeldingapi.vedlegg.VedleggService
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

@KtorExperimentalLocationsAPI
fun Route.søknadApis(
    søknadService: SøknadService,
    idTokenProvider: IdTokenProvider,
    barnService: BarnService,
    vedleggService: VedleggService
) {

    @Location(MELDING_URL_KORONAOVERFØRE)
    class sendSøknadForOverføring
    post { _: sendSøknadForOverføring ->
        logger.info("Mottatt ny melding om koronaoverføring av omsorgsdager.")

        logger.trace("Mapper melding")
        val melding = call.receive<Melding>()
        logger.trace("Melding mappet.")

       when(melding.korona) {
           null -> call.respondProblemDetails(ikkeKoronaOverføringsmelding)
           else -> {
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
       }
    }

    @Location(MELDING_URL_OVERFØRE)
    class sendSøknadOverføre
    post { _: sendSøknadOverføre ->
        logger.info("Mottatt ny melding om overføring av omsorgsdager.")

        logger.trace("Mapper melding")
        val melding = call.receive<Melding>()
        logger.trace("Melding mappet.")

        when (melding.overføring) {
            null -> {
                call.respondProblemDetails(ikkeOverføringmelding)
            }
            else -> {
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
        }
    }


    @Location(MELDING_URL_FORDELE)
    class sendSøknadForFordeling
    post { _: sendSøknadForFordeling ->
        logger.info("Mottatt ny melding om fordeling av omsorgsdager.")

        logger.trace("Mapper melding")
        val melding = call.receive<Melding>()

        when (melding.fordeling) {
            null -> {
                call.respondProblemDetails(ikkeFordelingsmelding)
            }
            else -> {
                logger.trace("Melding mappet.")

                logger.trace("Oppdaterer barn med identitetsnummer")
                val listeOverBarnMedFnr = barnService.hentNåværendeBarn(idTokenProvider.getIdToken(call), call.getCallId())
                melding.oppdaterBarnMedFnr(listeOverBarnMedFnr)
                logger.info("Oppdatering av identitetsnummer på barn OK")

                logger.trace("Validerer melding")
                melding.valider()

                when (melding.fordeling.samværsavtale.isEmpty()) {
                    true -> listOf()
                    else -> {
                        vedleggService.hentVedlegg(
                            idToken = idTokenProvider.getIdToken(call),
                            vedleggUrls = melding.fordeling.samværsavtale,
                            eier = DokumentEier(idTokenProvider.getIdToken(call).getSubject()!!),
                            callId = call.getCallId()
                        )
                    }
                }.validerVedlegg(melding.fordeling.samværsavtale)

                logger.trace("Validering OK.")

                logger.info(formaterStatuslogging(melding.søknadId, "validert OK"))

                val eier = idTokenProvider.getIdToken(call).getSubject()!!
                logger.info("Persisterer samværsavtale")
                vedleggService.persisterVedlegg(
                    vedleggsUrls = melding.fordeling.samværsavtale,
                    callId = call.getCallId(),
                    eier = DokumentEier(eier)
                )

                søknadService.registrer(
                    melding = melding,
                    metadata = call.metadata(),
                    callId = call.getCallId(),
                    idToken = idTokenProvider.getIdToken(call)
                )

                call.respond(HttpStatusCode.Accepted)
            }
        }
    }

}
