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
    val harBekreftetOpplysninger: Boolean,
    val mottakerFnr: String,
    val mottakerNavn: String,
    val harAleneomsorg: Boolean,
    val harUtvidetRett: Boolean,
    val erYrkesaktiv: Boolean,
    val arbeiderINorge: Boolean,
    val arbeidssituasjon: List<Arbeidssituasjon>,
    val antallDagerBruktIÅr: Int? = null,
    val barn: List<BarnUtvidet>,
    val type: Meldingstype,
    val korona: Koronaoverføre? = null,
    val overføring: Overføre? = null,
    val fordeling: KomplettFordele? = null
) {
    override fun toString(): String {
        return "KomplettMelding(mottatt=$mottatt, søknadId='$søknadId', id='$id')"
    }
}