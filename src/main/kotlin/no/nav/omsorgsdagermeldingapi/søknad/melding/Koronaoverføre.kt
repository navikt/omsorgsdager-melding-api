package no.nav.omsorgsdagermeldingapi.søknad.melding

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation
import java.time.LocalDate

val STENGINGSPERIODE_2021 = KoronaStengingsperiode(fraOgMed = LocalDate.parse("2021-01-01"), tilOgMed = LocalDate.parse("2021-12-31"))
val kjentePerioder = listOf(Pair(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-12-31")))

data class Koronaoverføre(
    val antallDagerSomSkalOverføres: Int,
    val stengingsperiode: KoronaStengingsperiode = STENGINGSPERIODE_2021
)

data class KoronaStengingsperiode(
    @JsonAlias("fom") @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonAlias("tom") @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate
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
        if (korona.antallDagerSomSkalOverføres !in MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE..MAX_ANTALL_DAGER_MAN_KAN_KORONA_OVERFØRE) {
            mangler.add(
                Violation(
                    parameterName = "korona.antallDagerSomSkalOverføres",
                    parameterType = ParameterType.ENTITY,
                    reason = "antallDagerSomSkalOverføres må være mellom $MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE og $MAX_ANTALL_DAGER_MAN_KAN_KORONA_OVERFØRE",
                    invalidValue = korona.antallDagerSomSkalOverføres
                )
            )
        }
        if (korona.stengingsperiode.ikkeErKjentPeriode()){
            mangler.add(
                Violation(
                    parameterName = "korona.stengingsperiode",
                    parameterType = ParameterType.ENTITY,
                    reason = "Stengingsperiode er ikke den kjente stengingsperioden som  er $STENGINGSPERIODE_2021",
                    invalidValue = korona.stengingsperiode
                )
            )
        }
    }

    return mangler
}
private fun KoronaStengingsperiode.ikkeErKjentPeriode(): Boolean =
    kjentePerioder.none { (it.first == fraOgMed && it.second == tilOgMed) }