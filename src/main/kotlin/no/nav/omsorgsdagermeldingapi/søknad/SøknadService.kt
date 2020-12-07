package no.nav.omsorgsdagermeldingapi.søknad

import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.omsorgsdagermeldingapi.felles.Metadata
import no.nav.omsorgsdagermeldingapi.felles.formaterStatuslogging
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import no.nav.omsorgsdagermeldingapi.kafka.SøknadKafkaProducer
import no.nav.omsorgsdagermeldingapi.søker.Søker
import no.nav.omsorgsdagermeldingapi.søker.SøkerService
import no.nav.omsorgsdagermeldingapi.søker.validate
import no.nav.omsorgsdagermeldingapi.søknad.melding.Melding
import no.nav.omsorgsdagermeldingapi.søknad.melding.Meldingstype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL


class SøknadService(
        private val søkerService: SøkerService,
        private val kafkaProducer: SøknadKafkaProducer,
        private val k9MellomLagringBaseUrl: URI,
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

        logger.trace("Henter søker")
        val søker: Søker = søkerService.getSøker(idToken = idToken, callId = callId)
        logger.trace("Søker hentet.")

        logger.trace("Validerer søker.")
        søker.validate()
        logger.trace("Søker OK.")

        val komplettMelding = melding.tilKomplettMelding(søker, k9MellomLagringBaseUrl)

        kafkaProducer.produce(komplettMelding = komplettMelding.copy()
                , metadata = metadata)
    }
}
