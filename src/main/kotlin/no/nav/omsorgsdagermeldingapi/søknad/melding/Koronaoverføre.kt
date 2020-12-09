package no.nav.omsorgsdagermeldingapi.søknad.melding

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation
import java.time.LocalDate

val kjentePerioder = listOf(
    Pair(LocalDate.parse("2020-03-13"), LocalDate.parse("2020-06-30")),
    Pair(LocalDate.parse("2020-08-10"), null)
)

data class Koronaoverføre(
    val antallDagerSomSkalOverføres: Int,
    val stengingsperiode: KoronaStengingsperiode
)

data class KoronaStengingsperiode(
    @JsonAlias("fom") @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonAlias("tom") @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate? = null
)

internal fun Melding.validerKoronaOverføre(): MutableSet<Violation> {
    val mangler: MutableSet<Violation> = mutableSetOf()
    if (korona == null) {
        mangler.add(
            Violation(
                parameterName = "korona",
                parameterType = ParameterType.ENTITY,
                reason = "korona kan ikke være null når type melding er koronaoverføring",
                invalidValue = korona
            )
        )
    } else {
        if (korona.antallDagerSomSkalOverføres !in MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE..MAX_ANTALL_DAGER_MAN_KAN_OVERFØRE) {
            mangler.add(
                Violation(
                    parameterName = "korona.antallDagerSomSkalOverføres",
                    parameterType = ParameterType.ENTITY,
                    reason = "antallDagerSomSkalOverføres må være mellom $MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE og $MAX_ANTALL_DAGER_MAN_KAN_OVERFØRE",
                    invalidValue = korona.antallDagerSomSkalOverføres
                )
            )
        }

        if (korona.stengingsperiode.ikkeErKjentPeriode()) {
            mangler.add(
                Violation(
                    parameterName = "korona.stengingsperiode",
                    parameterType = ParameterType.ENTITY,
                    reason = "stengingsperiode er ikke en kjent periode. Kjente perioder er: ${kjentePerioder}",
                    invalidValue = korona.stengingsperiode
                )
            )
        }
    }
    return mangler
}

private fun KoronaStengingsperiode.ikkeErKjentPeriode(): Boolean =
    kjentePerioder.none { (it.first == fraOgMed && it.second == tilOgMed) }
