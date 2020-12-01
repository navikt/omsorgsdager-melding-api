package no.nav.omsorgsdagermeldingapi.søknad.søknad

import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation

data class Medlemskap(
    val harBoddIUtlandetSiste12Mnd: Boolean? = null, //Settes til null for å unngå default false
    val utenlandsoppholdSiste12Mnd: List<Utenlandsopphold> = listOf(),
    val skalBoIUtlandetNeste12Mnd: Boolean? = null, //Settes til null for å unngå default false
    val utenlandsoppholdNeste12Mnd: List<Utenlandsopphold> = listOf()
)

internal fun Medlemskap.valider(): MutableSet<Violation> {
    val mangler: MutableSet<Violation> = mutableSetOf()

    mangler.addAll(nullSjekk(harBoddIUtlandetSiste12Mnd, "harBoddIUtlandetSiste12Mnd"))

    if(harBoddIUtlandetSiste12Mnd er true && utenlandsoppholdSiste12Mnd.isEmpty()){
        mangler.add(
            Violation(
                parameterName = "utenlandsoppholdSiste12Mnd",
                parameterType = ParameterType.ENTITY,
                reason = "Hvis harBoddIUtlandetSiste12Mnd er true så kan ikke utenlandsoppholdSiste12Mnd være en tom liste ",
                invalidValue = utenlandsoppholdSiste12Mnd
            )
        )
    }

    if(harBoddIUtlandetSiste12Mnd er false && utenlandsoppholdSiste12Mnd.isNotEmpty()){
        mangler.add(
            Violation(
                parameterName = "harBoddIUtlandetSiste12Mnd",
                parameterType = ParameterType.ENTITY,
                reason = "Hvis harBoddIUtlandetSiste12Mnd er false så kan ikke utenlandsoppholdSiste12Mnd inneholde noe ",
                invalidValue = harBoddIUtlandetSiste12Mnd
            )
        )
    }

    utenlandsoppholdSiste12Mnd.forEachIndexed{index, utenlandsopphold ->
        mangler.addAll(utenlandsopphold.valider(relatertFelt = "medlemskap.utenlandsoppholdSiste12Mnd[$index]"))
    }

    mangler.addAll(nullSjekk(skalBoIUtlandetNeste12Mnd, "skalBoIUtlandetNeste12Mnd"))

    if(skalBoIUtlandetNeste12Mnd er true && utenlandsoppholdNeste12Mnd.isEmpty()){
        mangler.add(
            Violation(
                parameterName = "utenlandsoppholdNeste12Mnd",
                parameterType = ParameterType.ENTITY,
                reason = "Hvis skalBoIUtlandetNeste12Mnd er true så kan ikke utenlandsoppholdNeste12Mnd være en tom liste ",
                invalidValue = utenlandsoppholdNeste12Mnd
            )
        )
    }

    if(skalBoIUtlandetNeste12Mnd er false && utenlandsoppholdNeste12Mnd.isNotEmpty()){
        mangler.add(
            Violation(
                parameterName = "skalBoIUtlandetNeste12Mnd",
                parameterType = ParameterType.ENTITY,
                reason = "Hvis skalBoIUtlandetNeste12Mnd er false så kan ikke utenlandsoppholdNeste12Mnd inneholde noe ",
                invalidValue = skalBoIUtlandetNeste12Mnd
            )
        )
    }

    utenlandsoppholdNeste12Mnd.forEachIndexed{index, utenlandsopphold ->
        mangler.addAll(utenlandsopphold.valider(relatertFelt = "medlemskap.utenlandsoppholdNeste12Mnd[$index]"))
    }

    return mangler
}