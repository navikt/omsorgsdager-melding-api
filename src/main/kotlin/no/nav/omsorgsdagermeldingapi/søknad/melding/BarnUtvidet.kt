package no.nav.omsorgsdagermeldingapi.søknad.melding

import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Violation
import java.time.LocalDate

data class BarnUtvidet(
    var identitetsnummer: String? = null,
    val aktørId: String?,
    val fødselsdato: LocalDate,
    val navn: String,
    val aleneOmOmsorgen: Boolean? = null, //Settes til null for å unngå default false
    val utvidetRett: Boolean ? = null //Settes til null for å unngå default false,
){
    override fun toString(): String {
        return "BarnUtvidet()"
    }

    fun manglerIdentitetsnummer(): Boolean = identitetsnummer.isNullOrEmpty()

    infix fun oppdaterIdentitetsnummerMed(identitetsnummer: String?){
        this.identitetsnummer = identitetsnummer
    }

    fun valider(index: Int): MutableSet<Violation> {
        val mangler: MutableSet<Violation> = mutableSetOf()

        if(identitetsnummer == null){
            mangler.add(
                Violation(
                    parameterName = "barn[$index].identitetsnummer",
                    parameterType = ParameterType.ENTITY,
                    reason = "Barn sittt identitetsnummer kan ikke være null",
                    invalidValue = identitetsnummer
                )
            )
        }

        if(identitetsnummer != null && identitetsnummer!!.erGyldigNorskIdentifikator() er false){
            mangler.add(
                Violation(
                    parameterName = "barn[$index].identitetsnummer",
                    parameterType = ParameterType.ENTITY,
                    reason = "Barn sittt identitetsnummer må være gyldig norsk identifikator",
                    invalidValue = identitetsnummer
                )
            )
        }

        if(navn.isNullOrBlank()){
            mangler.add(
                Violation(
                    parameterName = "barn[$index].navn",
                    parameterType = ParameterType.ENTITY,
                    reason = "Barn sittt navn må kan ikke være null, tom eller bare mellomrom",
                    invalidValue = navn
                )
            )
        }

        mangler.addAll(nullSjekk(aleneOmOmsorgen, "barn[$index].aleneOmOmsorgen"))
        mangler.addAll(nullSjekk(utvidetRett, "barn[$index].utvidetRett"))
        return mangler
    }
}