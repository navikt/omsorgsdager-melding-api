package no.nav.omsorgsdagermeldingapi.vedlegg

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.helse.dusseldorf.ktor.core.respondProblemDetails
import no.nav.omsorgsdagermeldingapi.felles.VEDLEGG_MED_ID_URL
import no.nav.omsorgsdagermeldingapi.felles.VEDLEGG_URL
import no.nav.omsorgsdagermeldingapi.general.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.general.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("nav.vedleggApis")

fun Route.vedleggApis(
    vedleggService: VedleggService,
    idTokenProvider: IdTokenProvider
) {

    delete(VEDLEGG_MED_ID_URL) {
        val vedleggId = VedleggId(call.parameters["vedleggId"]!!)
        logger.info("Sletter vedlegg")
        logger.info("$vedleggId")
        var eier = idTokenProvider.getIdToken(call).getSubject()
        if(eier == null) call.respond(HttpStatusCode.Forbidden) else {
            val resultat = vedleggService.slettVedlegg(
                vedleggId = vedleggId,
                idToken = idTokenProvider.getIdToken(call),
                callId = call.getCallId(),
                eier = DokumentEier(eier)
            )
            when(resultat){
                true -> call.respond(HttpStatusCode.NoContent)
                false -> call.respondProblemDetails(feilVedSlettingAvVedlegg)
            }
        }
    }

    get(VEDLEGG_MED_ID_URL) {
        val vedleggId = VedleggId(call.parameters["vedleggId"]!!)
        var eier = idTokenProvider.getIdToken(call).getSubject()

        if (eier == null) call.respond(HttpStatusCode.Forbidden) else {
            val vedlegg = vedleggService.hentVedlegg(
                vedleggId = vedleggId,
                idToken = idTokenProvider.getIdToken(call),
                callId = call.getCallId(),
                eier = DokumentEier(eier)
            )

            if(vedlegg == null) call.respondProblemDetails(vedleggNotFoundProblemDetails)
            else {
                call.respondBytes(
                    bytes = vedlegg.content,
                    contentType = ContentType.parse(vedlegg.contentType),
                    status = HttpStatusCode.OK
                )
            }
        }
    }

    post(VEDLEGG_URL) { _ ->
        logger.info("Lagrer vedlegg")
        if (!call.request.isFormMultipart()) {
            call.respondProblemDetails(hasToBeMultupartTypeProblemDetails)
        } else {
            val multipart = call.receiveMultipart()
            var vedlegg: Vedlegg? = null
            var eier = idTokenProvider.getIdToken(call).getSubject()
            if(eier == null){
                call.respondProblemDetails(fantIkkeSubjectPaaToken)
            } else {
                vedlegg = multipart.getVedlegg(DokumentEier(eier))
            }

            if (vedlegg == null) {
                call.respondProblemDetails(vedleggNotAttachedProblemDetails)
            } else if(!vedlegg.isSupportedContentType()) {
                call.respondProblemDetails(vedleggContentTypeNotSupportedProblemDetails)
            } else {
                if (vedlegg.content.size > MAX_VEDLEGG_SIZE) {
                    call.respondProblemDetails(vedleggTooLargeProblemDetails)
                } else {
                    val vedleggId = vedleggService.lagreVedlegg(
                        vedlegg = vedlegg,
                        idToken = idTokenProvider.getIdToken(call),
                        callId = call.getCallId()
                    )
                    logger.info("$vedleggId")
                    call.respondVedlegg(vedleggId)
                }
            }
        }
    }
}


private suspend fun MultiPartData.getVedlegg(eier: DokumentEier) : Vedlegg? {
    for (partData in readAllParts()) {
        if (partData is PartData.FileItem && "vedlegg".equals(partData.name, ignoreCase = true) && partData.contentType != null) {
            val vedlegg = Vedlegg(
                content = partData.streamProvider().readBytes(),
                contentType = partData.contentType.toString(),
                title = partData.originalFileName?: "Ingen tittel tilgjengelig",
                eier = eier
            )
            partData.dispose()
            return vedlegg
        }
        partData.dispose()
    }
    return null
}


private fun Vedlegg.isSupportedContentType(): Boolean = supportedContentTypes.contains(contentType.lowercase())

private fun ApplicationRequest.isFormMultipart(): Boolean {
    return contentType().withoutParameters().match(ContentType.MultiPart.FormData)
}

private suspend fun ApplicationCall.respondVedlegg(vedleggId: VedleggId) {
    val url = URLBuilder(getBaseUrlFromRequest()).path("vedlegg",vedleggId.value).build().toString()
    response.header(HttpHeaders.Location, url)
    response.header(HttpHeaders.AccessControlExposeHeaders, HttpHeaders.Location)
    respond(HttpStatusCode.Created)
}

private fun ApplicationCall.getBaseUrlFromRequest() : String {
    val host = request.origin.host
    val isLocalhost = "localhost".equals(host, ignoreCase = true)
    val scheme = if (isLocalhost) "http" else "https"
    val port = if (isLocalhost) ":${request.origin.port}" else ""
    return "$scheme://$host$port"
}
