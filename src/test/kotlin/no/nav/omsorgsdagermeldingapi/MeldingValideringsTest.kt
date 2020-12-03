package no.nav.omsorgsdagermeldingapi

import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.omsorgsdagermeldingapi.søknad.melding.starterMedFodselsdato
import no.nav.omsorgsdagermeldingapi.søknad.melding.valider
import org.junit.Test
import kotlin.test.assertTrue

internal class MeldingValideringsTest {

    companion object {
        private val ugyldigFødselsnummer = "12345678900"
    }

    //Felles
    @Test
    fun `Tester gyldig fødselsdato dersom dnunmer`() {
        val starterMedFodselsdato = "630293".starterMedFodselsdato()
        assertTrue(starterMedFodselsdato)
    }

    @Test
    fun `Gyldig søknad`() {
        val søknad = MeldingUtils.gyldigMeldingKoronaoverføre
        søknad.valider()
    }


    @Test(expected = Throwblem::class)
    fun `Feiler dersom harForståttRettigheterOgPlikter er false`(){
        val søknad = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harForståttRettigheterOgPlikter = false
        )
        søknad.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harBekreftetOpplysninger er false`(){
        val søknad = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harBekreftetOpplysninger = false
        )
        søknad.valider()
    }

    //Koronaoverføring

    //Overføring

    //Fordeling

}