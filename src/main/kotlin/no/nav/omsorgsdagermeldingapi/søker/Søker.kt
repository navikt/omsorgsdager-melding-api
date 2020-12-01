package no.nav.omsorgsdagermeldingapi.søker

import java.time.LocalDate
import java.time.ZoneId

private val ZONE_ID = ZoneId.of("Europe/Oslo")
private const val MYNDIG_ALDER = 18L

data class Søker (
    val aktørId: String,
    val fødselsdato: LocalDate,
    val fødselsnummer: String,
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null,
    val myndig: Boolean = erMyndig(fødselsdato)
)

private fun erMyndig(fodselsdato: LocalDate) : Boolean {
    val myndighetsDato = fodselsdato.plusYears(MYNDIG_ALDER)
    val dagensDato = LocalDate.now(ZONE_ID)
    return myndighetsDato.isBefore(dagensDato) || myndighetsDato.isEqual(dagensDato)
}

