package no.nav.omsorgsdagermeldingapi.søknad.melding

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.omsorgsdagermeldingapi.søker.Søker
import java.net.URL
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

data class Melding(
    val søknadId: String = UUID.randomUUID().toString(),
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
    val antallDagerBruktEtter1Juli: Int,
    val barn: List<BarnUtvidet>,
    val type: Meldingstype,
    val korona: Koronaoverføre? = null,
    val overføring: Overføre? = null,
    val fordeling: Fordele? = null
) {
    fun tilKomplettMelding(søker: Søker): KomplettMelding = KomplettMelding(
        mottatt = ZonedDateTime.now(ZoneOffset.UTC),
        søker = søker,
        søknadId = søknadId,
        id = id,
        språk = språk,
        harBekreftetOpplysninger = harBekreftetOpplysninger,
        harForståttRettigheterOgPlikter = harForståttRettigheterOgPlikter,
        mottakerFnr = mottakerFnr,
        mottakerNavn = mottakerNavn,
        harUtvidetRett = harUtvidetRett,
        harAleneomsorg = harAleneomsorg,
        erYrkesaktiv = erYrkesaktiv,
        arbeiderINorge = arbeiderINorge,
        arbeidssituasjon = arbeidssituasjon,
        antallDagerBruktEtter1Juli = antallDagerBruktEtter1Juli,
        barn = barn,
        type = type,
        korona = korona,
        overføring = overføring,
        fordeling = fordeling
    )
}

data class Koronaoverføre(
    val antallDagerSomSkalOverføres: Int
)

data class Overføre(
    val mottakerType: Mottaker,
    val antallDagerSomSkalOverføres: Int
)

data class Fordele(
    val mottakerType: Mottaker,
    val samværsavtale: List<URL>? = null
)

enum class Mottaker() {
    @JsonAlias("ektefelle") EKTEFELLE,
    @JsonAlias("samboer") SAMBOER,
    @JsonAlias("samværsforelder") SAMVÆRSFORELDER
}
enum class Arbeidssituasjon(){
    @JsonAlias("selvstendigNæringsdrivende") SELVSTENDIG_NÆRINGSDRIVENDE,
    @JsonAlias("arbeidstaker") ARBEIDSTAKER,
    @JsonAlias("frilanser") FRILANSER,
    @JsonAlias("annen") ANNEN
}

enum class Meldingstype(){
    @JsonAlias("koronaoverføring") KORONA_OVERFØRE,
    @JsonAlias("overføring") OVERFØRE,
    @JsonAlias("fordeling") FORDELE
}