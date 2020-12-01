package no.nav.omsorgsdagermeldingapi.felles

data class Metadata(
    val version : Int,
    val correlationId : String,
    val requestId : String
)