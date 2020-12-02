package no.nav.omsorgsdagermeldingapi

import no.nav.omsorgsdagermeldingapi.søknad.melding.*
import java.net.URL
import java.time.LocalDate

object MeldingUtils {

    val gyldigMeldingMal = Melding(
        id = "01ARZ3NDEKTSV4RRFFQ69G5FAV",
        språk = "nb",
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true,
        mottakerNavn = "Berit",
        mottakerFnr = "26104500284",
        harAleneomsorg = true,
        harUtvidetRett = true,
        erYrkesaktiv = true,
        arbeiderINorge = true,
        arbeidssituasjon = listOf(Arbeidssituasjon.ARBEIDSTAKER, Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE),
        antallDagerBruktEtter1Juli = 1,
        barn = listOf(
            BarnUtvidet(
                identitetsnummer = "02047816826",
                navn = "Kjell",
                aleneOmOmsorgen = true,
                utvidetRett = true,
                fødselsdato = LocalDate.parse("2020-01-01"),
                aktørId = "12345"
            )
        ),
        type = Meldingstype.KORONA_OVERFØRE
    )

    val gyldigMeldingKoronaoverføre = gyldigMeldingMal.copy(
        type = Meldingstype.KORONA_OVERFØRE,
        korona = Koronaoverføre(
            antallDagerSomSkalOverføres = 5
        )
    )

    val gyldigMeldingOverføre = gyldigMeldingMal.copy(
        type = Meldingstype.OVERFØRE,
        overføring = Overføre(
            mottakerType = Mottaker.EKTEFELLE,
            antallDagerSomSkalOverføres = 7
        )
    )

    val gyldigMeldingFordele = gyldigMeldingMal.copy(
        type = Meldingstype.FORDELE,
        fordeling = Fordele(
            mottakerType = Mottaker.SAMVÆRSFORELDER,
            samværsavtale = listOf(URL("http://localhost:8080/vedlegg/1"))
        )
    )
}