package no.nav.omsorgsdagermeldingapi.søknad.melding

import no.nav.omsorgsdagermeldingapi.søker.Søker
import java.time.ZonedDateTime

data class KomplettMelding(
    val mottatt: ZonedDateTime,
    val søker: Søker,
    val søknadId: String,
    val id: String,
    val språk: String,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean
)