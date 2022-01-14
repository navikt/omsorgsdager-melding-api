package no.nav.omsorgsdagermeldingapi.søknad.melding

import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation
import java.net.URL

data class Fordele(
    val mottakerType: Mottaker,
    val samværsavtale: List<URL> = listOf()
)

data class KomplettFordele(
    val mottakerType: Mottaker,
    val samværsavtaleVedleggId: List<String> = listOf()
)

internal fun Melding.validerFordele(): MutableSet<Violation> {
    val mangler: MutableSet<Violation> = mutableSetOf()
    if (fordeling == null) {
        mangler.add(
            Violation(
                parameterName = "fordeling",
                parameterType = ParameterType.ENTITY,
                reason = "fordeling kan ikke være null når type melding er fordele",
                invalidValue = fordeling
            )
        )
    } else {
        if (fordeling.mottakerType != Mottaker.SAMVÆRSFORELDER) {
            mangler.add(
                Violation(
                    parameterName = "fordeling.mottakerType",
                    parameterType = ParameterType.ENTITY,
                    reason = "mottakerType må være samværsforelder",
                    invalidValue = fordeling.mottakerType
                )
            )
        }
    }

    return mangler
}
