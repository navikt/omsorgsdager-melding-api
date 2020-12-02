package no.nav.omsorgsdagermeldingapi.søknad.melding

import no.nav.omsorgsdagermeldingapi.søker.Søker
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

data class Melding(
    val søknadId: String = UUID.randomUUID().toString(),
    val id: String,
    val språk: String,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean
) {
    fun tilKomplettMelding(søker: Søker): KomplettMelding = KomplettMelding(
            mottatt = ZonedDateTime.now(ZoneOffset.UTC),
            søker = søker,
            søknadId = søknadId,
            id = id,
            språk = språk,
            harBekreftetOpplysninger = harBekreftetOpplysninger,
            harForståttRettigheterOgPlikter = harForståttRettigheterOgPlikter
        )
}