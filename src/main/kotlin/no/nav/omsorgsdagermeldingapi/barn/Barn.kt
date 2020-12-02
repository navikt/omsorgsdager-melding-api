package no.nav.omsorgsdagermeldingapi.barn

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate

data class BarnResponse(
    val barn: List<Barn>
)

data class Barn (
    val fødselsdato: LocalDate,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
    val aktørId: String?,
    @JsonIgnore var identitetsnummer: String? = null
)