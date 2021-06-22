package no.nav.omsorgsdagermeldingapi.søknad

import no.nav.omsorgsdagermeldingapi.barn.BarnService
import no.nav.omsorgsdagermeldingapi.felles.Metadata
import no.nav.omsorgsdagermeldingapi.felles.formaterStatuslogging
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import no.nav.omsorgsdagermeldingapi.kafka.SøknadKafkaProducer
import no.nav.omsorgsdagermeldingapi.søker.Søker
import no.nav.omsorgsdagermeldingapi.søker.SøkerService
import no.nav.omsorgsdagermeldingapi.søker.validate
import no.nav.omsorgsdagermeldingapi.søknad.melding.Melding
import no.nav.omsorgsdagermeldingapi.søknad.melding.valider
import no.nav.omsorgsdagermeldingapi.søknad.melding.validerVedlegg
import no.nav.omsorgsdagermeldingapi.vedlegg.DokumentEier
import no.nav.omsorgsdagermeldingapi.vedlegg.VedleggService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

class SøknadService(
    private val søkerService: SøkerService,
    private val barnService: BarnService,
    private val kafkaProducer: SøknadKafkaProducer,
    private val k9MellomLagringIngress: URI,
    private val vedleggService: VedleggService
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SøknadService::class.java)
    }

    suspend fun registrer(
        melding: Melding,
        metadata: Metadata,
        idToken: IdToken,
        callId: CallId,
    ) {
        logger.info(formaterStatuslogging(melding.søknadId, "registreres"))

        val listeOverBarnMedFnr = barnService.hentNåværendeBarn(idToken, callId)
        melding.oppdaterBarnMedFnr(listeOverBarnMedFnr)
        melding.valider()

        val søker: Søker = søkerService.getSøker(idToken, callId)
        søker.validate()

        if (melding.fordeling != null) fordelingValiderVedlegg(melding, idToken, callId, søker)

        val komplettMelding = melding.tilKomplettMelding(søker, k9MellomLagringIngress)

        try {
            kafkaProducer.produserKafkaMelding(komplettMelding = komplettMelding, metadata = metadata)
        } catch (exception: Exception) {
            logger.info("Feilet ved å legge melding på Kafka. Sletter persistert samværsavtale hvis det eksisterer")

            if (melding.fordeling != null && melding.fordeling.samværsavtale.isNotEmpty())
                vedleggService.slettPersistertVedlegg(
                    vedleggsUrls = melding.fordeling.samværsavtale,
                    callId = callId,
                    eier = DokumentEier(søker.fødselsnummer)
                )

            throw MeldingRegistreringFeiletException("Feilet ved å legge melding på Kafka")
        }
    }

    private suspend fun fordelingValiderVedlegg(
        melding: Melding,
        idToken: IdToken,
        callId: CallId,
        søker: Søker
    ) {
        if (melding.fordeling != null) {
            when (melding.fordeling.samværsavtale.isEmpty()) {
                true -> listOf()
                else -> {
                    vedleggService.hentVedlegg(
                        idToken = idToken,
                        vedleggUrls = melding.fordeling.samværsavtale,
                        eier = DokumentEier(søker.fødselsnummer),
                        callId = callId
                    )
                }
            }.validerVedlegg(melding.fordeling.samværsavtale)
        }
    }
}

class MeldingRegistreringFeiletException(s: String) : Throwable(s)