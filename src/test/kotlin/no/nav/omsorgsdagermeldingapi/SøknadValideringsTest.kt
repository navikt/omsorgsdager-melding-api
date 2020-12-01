package no.nav.omsorgsdagermeldingapi

import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.omsorgsdagermeldingapi.søknad.søknad.Utenlandsopphold
import no.nav.omsorgsdagermeldingapi.søknad.søknad.starterMedFodselsdato
import no.nav.omsorgsdagermeldingapi.søknad.søknad.valider
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertTrue

internal class SøknadValideringsTest {

    companion object {
        private val ugyldigFødselsnummer = "12345678900"
    }

    @Test
    fun `Tester gyldig fødselsdato dersom dnunmer`() {
        val starterMedFodselsdato = "630293".starterMedFodselsdato()
        assertTrue(starterMedFodselsdato)
    }

    @Test
    fun `Gyldig søknad`() {
        val søknad = SøknadUtils.gyldigSøknad
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom antall barn er 0 eller mindre`(){
        SøknadUtils.gyldigSøknad.copy(
            antallBarn = 0
        ).valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harForståttRettigheterOgPlikter er false`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            harForståttRettigheterOgPlikter = false
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harBekreftetOpplysninger er false`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            harBekreftetOpplysninger = false
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom annen forelder har ugyldig fnr`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            annenForelder = SøknadUtils.gyldigSøknad.annenForelder.copy(
                fnr = ugyldigFødselsnummer
            )
        )
        søknad.valider()
    }

    @Test(expected =  Throwblem::class)
    fun `Feiler dersom annen forelder sitt navn er ugydlig`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            annenForelder = SøknadUtils.gyldigSøknad.annenForelder.copy(
                navn = "   "
            )
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom fødselsår på barn er høyere enn året vi er i`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            fødselsårBarn = listOf(
                LocalDate.now().year.plus(1)
            )
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom medlemskap har et Utenlandsopphold som har ugydlig landnavn`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            medlemskap = SøknadUtils.gyldigSøknad.medlemskap.copy(
                utenlandsoppholdNeste12Mnd = listOf(
                    Utenlandsopphold(
                        fraOgMed = LocalDate.now(),
                        tilOgMed = LocalDate.now().plusDays(1),
                        landnavn = "  ",
                        landkode = "GE"
                    )
                )
            )
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom medlemskap har et Utenlandsopphold som har ugydlig landkode`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            medlemskap = SøknadUtils.gyldigSøknad.medlemskap.copy(
                utenlandsoppholdNeste12Mnd = listOf(
                    Utenlandsopphold(
                        fraOgMed = LocalDate.now(),
                        tilOgMed = LocalDate.now().plusDays(1),
                        landnavn = "Sverige",
                        landkode = " "
                    )
                )
            )
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom medlemskap har et Utenlandsopphold hvor fraOgMed er etter tilOgMed`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            medlemskap = SøknadUtils.gyldigSøknad.medlemskap.copy(
                utenlandsoppholdNeste12Mnd = listOf(
                    Utenlandsopphold(
                        fraOgMed = LocalDate.now(),
                        tilOgMed = LocalDate.now().minusDays(1),
                        landnavn = "Sverige",
                        landkode = "SWE"
                    )
                )
            )
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom medlemskap har harBoddIUtlandetSiste12Mnd til true men listen er tom `(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            medlemskap = SøknadUtils.gyldigSøknad.medlemskap.copy(
                skalBoIUtlandetNeste12Mnd = true,
                utenlandsoppholdNeste12Mnd = listOf()
            )
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom medlemskap har harBoddIUtlandetSiste12Mnd til false men listen inneholder et element`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            medlemskap = SøknadUtils.gyldigSøknad.medlemskap.copy(
                skalBoIUtlandetNeste12Mnd = false,
                utenlandsoppholdNeste12Mnd = listOf(
                    Utenlandsopphold(
                        fraOgMed = LocalDate.now(),
                        tilOgMed = LocalDate.now().plusDays(1),
                        landnavn = "Sverige",
                        landkode = "SWE"
                    )
                )
            )
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom medlemskap har harBoddIUtlandetSiste12Mnd til null`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            medlemskap = SøknadUtils.gyldigSøknad.medlemskap.copy(
                skalBoIUtlandetNeste12Mnd = null,
                utenlandsoppholdNeste12Mnd = listOf()
            )
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom arbedissituasjon er tom`(){
        val søknad = SøknadUtils.gyldigSøknad.copy(
            arbeidssituasjon = listOf()
        )
        søknad.valider()
    }

}
