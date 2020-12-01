package no.nav.omsorgsdagermeldingapi

import no.nav.omsorgsdagermeldingapi.søknad.søknad.*
import java.time.LocalDate

object SøknadUtils {

    val gyldigSøknad = Søknad(
        id = "123456789",
        språk = "nb",
        arbeidssituasjon = listOf(Arbeidssituasjon.FRILANSER),
        annenForelder = AnnenForelder(
            navn = "Berit",
            fnr = "02119970078",
            situasjon = Situasjon.FENGSEL,
            situasjonBeskrivelse = "Sitter i fengsel..",
            periodeOver6Måneder = false,
            periodeFraOgMed = LocalDate.parse("2020-01-01"),
            periodeTilOgMed = LocalDate.parse("2020-10-01")
        ),
        antallBarn = 2,
        fødselsårBarn = listOf(2005, 2013),
        medlemskap = Medlemskap(
            harBoddIUtlandetSiste12Mnd = true,
            utenlandsoppholdSiste12Mnd = listOf(
                Utenlandsopphold(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-10"),
                    landnavn = "Tyskland",
                    landkode = "DE"
                ),
                Utenlandsopphold(
                    fraOgMed = LocalDate.parse("2020-01-01"),
                    tilOgMed = LocalDate.parse("2020-01-10"),
                    landnavn = "Sverige",
                    landkode = "SWE"
                )
            ),
            skalBoIUtlandetNeste12Mnd = true,
            utenlandsoppholdNeste12Mnd = listOf(
                Utenlandsopphold(
                    fraOgMed = LocalDate.parse("2020-10-01"),
                    tilOgMed = LocalDate.parse("2020-10-10"),
                    landnavn = "Brasil",
                    landkode = "BR"
                )
            )
        ),
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true
    )
}