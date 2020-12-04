package no.nav.omsorgsdagermeldingapi.søknad.melding

import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation

data class Overføre(
    val mottakerType: Mottaker,
    val antallDagerSomSkalOverføres: Int
)

internal fun Melding.validerOverføre(): MutableSet<Violation> {
    val mangler: MutableSet<Violation> = mutableSetOf()
    if(overføring == null){
        mangler.add(
            Violation(
                parameterName = "overføring",
                parameterType = ParameterType.ENTITY,
                reason = "overføring kan ikke være null når type melding er overføre",
                invalidValue = overføring
            )
        )
    } else {
        if(overføring.antallDagerSomSkalOverføres !in MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE..MAX_ANTALL_DAGER_MAN_KAN_OVERFØRE){
            mangler.add(
                Violation(
                    parameterName = "overføring.antallDagerSomSkalOverføres",
                    parameterType = ParameterType.ENTITY,
                    reason = "antallDagerSomSkalOverføres må være mellom $MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE og $MAX_ANTALL_DAGER_MAN_KAN_OVERFØRE",
                    invalidValue = overføring.antallDagerSomSkalOverføres
                )
            )
        }

        if(overføring.mottakerType != Mottaker.EKTEFELLE && overføring.mottakerType != Mottaker.SAMBOER){
            mangler.add(
                Violation(
                    parameterName = "overføring.mottakerType",
                    parameterType = ParameterType.ENTITY,
                    reason = "overføring.mottakerType må enten være ektefelle eller samboer",
                    invalidValue = overføring.mottakerType
                )
            )
        }
    }
    return mangler
}
