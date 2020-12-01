package no.nav.omsorgsdagermeldingapi.søknad

import no.nav.omsorgsdagermeldingapi.felles.Metadata
import no.nav.omsorgsdagermeldingapi.felles.formaterStatuslogging
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import no.nav.omsorgsdagermeldingapi.kafka.SøknadKafkaProducer
import no.nav.omsorgsdagermeldingapi.søker.Søker
import no.nav.omsorgsdagermeldingapi.søker.SøkerService
import no.nav.omsorgsdagermeldingapi.søker.validate
import no.nav.omsorgsdagermeldingapi.søknad.søknad.Søknad
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class SøknadService(
    private val søkerService: SøkerService,
    private val kafkaProducer: SøknadKafkaProducer
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(SøknadService::class.java)
    }

    suspend fun registrer(
        søknad: Søknad,
        metadata: Metadata,
        idToken: IdToken,
        callId: CallId
    ) {
        logger.info(formaterStatuslogging(søknad.søknadId, "registreres"))

        logger.trace("Henter søker")
        val søker: Søker = søkerService.getSøker(idToken = idToken, callId = callId)
        logger.trace("Søker hentet.")

        logger.trace("Validerer søker.")
        søker.validate()
        logger.trace("Søker OK.")

        val komplettSøknad = søknad.tilKomplettSøknad(søker)

        kafkaProducer.produce(søknad = komplettSøknad, metadata = metadata)
    }
}