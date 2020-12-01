package no.nav.omsorgsdagermeldingapi.søknad.søknad

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation
import java.time.LocalDate

data class Utenlandsopphold(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String
)

internal fun Utenlandsopphold.valider(relatertFelt: String): MutableSet<Violation> {
    val mangler: MutableSet<Violation> = mutableSetOf()

    if(tilOgMed.isBefore(fraOgMed)){
        mangler.add(
            Violation(
                parameterName = "$relatertFelt.tilOgMed",
                parameterType = ParameterType.ENTITY,
                reason = "tilOgMed kan ikke være før fraOgMed",
                invalidValue = tilOgMed
            )
        )
    }

    if(landkode.isNullOrBlank()){
        mangler.add(
            Violation(
                parameterName = "$relatertFelt.landkode",
                parameterType = ParameterType.ENTITY,
                reason = "Landkode er ikke gyldig",
                invalidValue = landkode
            )
        )
    }

    if(landnavn.isNullOrBlank()){
        mangler.add(
            Violation(
                parameterName = "$relatertFelt.landnavn",
                parameterType = ParameterType.ENTITY,
                reason = "Landnavn er ikke gyldig",
                invalidValue = landnavn
            )
        )
    }

    return mangler
}