package no.nav.omsorgsdagermeldingapi

import no.nav.omsorgsdagermeldingapi.søknad.søknad.*
import java.time.LocalDate

object SøknadUtils {

    val gyldigSøknad = Søknad(
        id = "123456789",
        språk = "nb",
        harBekreftetOpplysninger = true,
        harForståttRettigheterOgPlikter = true
    )
}