package no.nav.omsorgsdagermeldingapi.søker

import no.nav.helse.dusseldorf.ktor.core.DefaultProblemDetails
import no.nav.helse.dusseldorf.ktor.core.Throwblem

internal fun Søker.validate() {
    if (!myndig) {
        throw Throwblem(DefaultProblemDetails(
            title = "unauthorized",
            status = 403,
            detail = "Søkeren er ikke myndig og kan ikke sende inn melding."
        ))
    }
}