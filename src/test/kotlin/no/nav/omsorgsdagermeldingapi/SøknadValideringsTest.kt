package no.nav.omsorgsdagermeldingapi

import no.nav.helse.dusseldorf.ktor.core.Throwblem
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

}
