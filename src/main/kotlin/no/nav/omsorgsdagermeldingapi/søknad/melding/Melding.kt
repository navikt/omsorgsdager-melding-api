package no.nav.omsorgsdagermeldingapi.søknad.melding

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.omsorgsdagermeldingapi.barn.Barn
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
    val harAleneomsorg: Boolean? = null,
    val harUtvidetRett: Boolean? = null,
    val erYrkesaktiv: Boolean? = null,
    val arbeiderINorge: Boolean? = null,
    val arbeidssituasjon: List<Arbeidssituasjon>,
    @JsonAlias("antallDagerBruktEtter1Juli")
    val antallDagerBruktIÅr: Int? = null,
    val barn: List<BarnUtvidet>,
    val type: Meldingstype,
    val korona: Koronaoverføre? = null,
    val overføring: Overføre? = null,
    val fordeling: Fordele? = null,
) {
    override fun toString(): String {
        return "Melding(søknadId='$søknadId', id='$id')"
    }

    fun oppdaterBarnMedFnr(listeOverBarn: List<Barn>) {
        barn.forEach { barn ->
            if (barn.manglerIdentitetsnummer()) {
                barn oppdaterIdentitetsnummerMed listeOverBarn.hentIdentitetsnummerForBarn(barn.aktørId)
            }
        }
    }

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
        harUtvidetRett = harUtvidetRett!!,
        harAleneomsorg = harAleneomsorg!!,
        erYrkesaktiv = erYrkesaktiv!!,
        arbeiderINorge = arbeiderINorge!!,
        arbeidssituasjon = arbeidssituasjon,
        antallDagerBruktIÅr = antallDagerBruktIÅr,
        barn = barn,
        type = type,
        korona = korona,
        overføring = overføring,
        fordeling = fordeling?.let {
            KomplettFordele(
                it.mottakerType,
                it.samværsavtale.map { url ->
                    url.vedleggId()
                }
            )
        }
    )
}

fun URL.vedleggId(): String = this.toString().substringAfterLast("/")
fun String.vedleggId(): String = substringAfterLast("/")

private fun List<Barn>.hentIdentitetsnummerForBarn(aktørId: String?): String? {
    this.forEach {
        if (it.aktørId == aktørId) return it.identitetsnummer
    }
    return null
}

enum class Mottaker() {
    @JsonAlias("ektefelle")
    EKTEFELLE,

    @JsonAlias("samboer")
    SAMBOER,

    @JsonAlias("samværsforelder")
    SAMVÆRSFORELDER
}

enum class Arbeidssituasjon() {
    @JsonAlias("selvstendigNæringsdrivende")
    SELVSTENDIG_NÆRINGSDRIVENDE,

    @JsonAlias("arbeidstaker")
    ARBEIDSTAKER,

    @JsonAlias("frilanser")
    FRILANSER,

    @JsonAlias("annen")
    ANNEN
}

enum class Meldingstype() {
    @JsonAlias("koronaoverføring")
    KORONA,

    @JsonAlias("overføring")
    OVERFORING,

    @JsonAlias("fordeling")
    FORDELING
}
