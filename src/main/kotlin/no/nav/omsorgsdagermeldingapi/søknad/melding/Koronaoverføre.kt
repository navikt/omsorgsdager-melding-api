package no.nav.omsorgsdagermeldingapi.søknad.melding

import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation

data class Koronaoverføre(
    val antallDagerSomSkalOverføres: Int
)

internal fun Melding.validerKoronaOverføre(): MutableSet<Violation> {
    val mangler: MutableSet<Violation> = mutableSetOf()
    if(korona == null){
        mangler.add(
            Violation(
                parameterName = "korona",
                parameterType = ParameterType.ENTITY,
                reason = "korona kan ikke være null når type melding er koronaoverføring",
                invalidValue = korona
            )
        )
    } else {
        if(korona.antallDagerSomSkalOverføres !in MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE..MAX_ANTALL_DAGER_MAN_KAN_OVERFØRE){
            mangler.add(
                Violation(
                    parameterName = "korona.antallDagerSomSkalOverføres",
                    parameterType = ParameterType.ENTITY,
                    reason = "antallDagerSomSkalOverføres må være mellom $MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE og $MAX_ANTALL_DAGER_MAN_KAN_OVERFØRE",
                    invalidValue = korona.antallDagerSomSkalOverføres
                )
            )
        }
    }
    return mangler
}