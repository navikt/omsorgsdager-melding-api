package no.nav.omsorgsdagermeldingapi.søknad.søknad

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.omsorgsdagermeldingapi.søker.Søker
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

data class Søknad(
    val søknadId: String = UUID.randomUUID().toString(),
    val id: String,
    val språk: String,
    val harForståttRettigheterOgPlikter: Boolean,
    val harBekreftetOpplysninger: Boolean
) {
    fun tilKomplettSøknad(søker: Søker): KomplettSøknad = KomplettSøknad(
            mottatt = ZonedDateTime.now(ZoneOffset.UTC),
            søker = søker,
            søknadId = søknadId,
            id = id,
            språk = språk,
            harBekreftetOpplysninger = harBekreftetOpplysninger,
            harForståttRettigheterOgPlikter = harForståttRettigheterOgPlikter
        )

}

enum class Arbeidssituasjon(){
    @JsonAlias("selvstendigNæringsdrivende") SELVSTENDIG_NÆRINGSDRIVENDE,
    @JsonAlias("arbeidstaker") ARBEIDSTAKER,
    @JsonAlias("frilanser") FRILANSER,
    @JsonAlias("annen") ANNEN
}