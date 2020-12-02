package no.nav.omsorgsdagermeldingapi.søknad.melding

import java.time.LocalDate

data class BarnUtvidet(
    var identitetsnummer: String?,
    val aktørId: String?,
    val fødselsdato: LocalDate,
    val navn: String,
    val aleneOmOmsorgen: Boolean? = null, //Settes til null for å unngå default false
    val utvidetRett: Boolean ? = null //Settes til null for å unngå default false,
){
    fun manglerIdentitetsnummer(): Boolean = identitetsnummer.isNullOrEmpty()

    infix fun oppdaterIdentitetsnummerMed(identitetsnummer: String?){
        this.identitetsnummer = identitetsnummer
    }
}