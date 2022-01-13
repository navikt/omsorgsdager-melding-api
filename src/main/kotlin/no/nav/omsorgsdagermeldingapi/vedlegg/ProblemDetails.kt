package no.nav.omsorgsdagermeldingapi.vedlegg

import no.nav.helse.dusseldorf.ktor.core.DefaultProblemDetails

const val MAX_VEDLEGG_SIZE = 8 * 1024 * 1024
val supportedContentTypes = listOf("application/pdf", "image/jpeg", "image/png")
val hasToBeMultupartTypeProblemDetails = DefaultProblemDetails(
    title = "multipart-form-required",
    status = 400,
    detail = "Requesten må være en 'multipart/form-data' request hvor en 'part' er en fil, har 'name=vedlegg' og har Content-Type header satt."
)
val vedleggNotAttachedProblemDetails = DefaultProblemDetails(
    title = "attachment-not-attached",
    status = 400,
    detail = "Fant ingen 'part' som er en fil, har 'name=vedlegg' og har Content-Type header satt."
)
val vedleggTooLargeProblemDetails = DefaultProblemDetails(
    title = "attachment-too-large",
    status = 413,
    detail = "vedlegget var over maks tillatt størrelse på 8MB."
)

val vedleggContentTypeNotSupportedProblemDetails = DefaultProblemDetails(
    title = "attachment-content-type-not-supported",
    status = 400,
    detail = "Vedleggets type må være en av $supportedContentTypes"
)
internal val feilVedSlettingAvVedlegg =
    DefaultProblemDetails(title = "feil-ved-sletting", status = 500, detail = "Feil ved sletting av vedlegg")
val vedleggNotFoundProblemDetails = DefaultProblemDetails(
    title = "attachment-not-found",
    status = 404,
    detail = "Inget vedlegg funnet med etterspurt ID."
)