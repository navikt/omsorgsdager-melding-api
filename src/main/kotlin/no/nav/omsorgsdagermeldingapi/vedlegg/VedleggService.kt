package no.nav.omsorgsdagermeldingapi.vedlegg

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.general.auth.IdToken
import java.net.URL

class VedleggService(
    private val k9MellomlagringGateway: K9MellomlagringGateway
) {

    suspend fun lagreVedlegg(
        vedlegg: Vedlegg,
        idToken: IdToken,
        callId: CallId
    ) : VedleggId {

        return k9MellomlagringGateway.lagreVedlegg(
            vedlegg = vedlegg,
            idToken = idToken,
            callId = callId
        )

    }

    suspend fun hentVedlegg(
        vedleggId: VedleggId,
        idToken: IdToken,
        callId: CallId
    ) : Vedlegg? {

        return k9MellomlagringGateway.hentVedlegg(
            vedleggId = vedleggId,
            idToken = idToken,
            callId = callId
        )
    }

    suspend fun hentVedlegg(
        vedleggUrls: List<URL>,
        idToken: IdToken,
        callId: CallId
    ) : List<Vedlegg> {
        val vedlegg = coroutineScope {
            val futures = mutableListOf<Deferred<Vedlegg?>>()
            vedleggUrls.forEach {
                futures.add(async { hentVedlegg(
                    vedleggId = vedleggIdFromUrl(it),
                    idToken = idToken,
                    callId = callId
                )})

            }
            futures.awaitAll().filter { it != null }
        }
        return vedlegg.requireNoNulls()
    }

    suspend fun slettVedlegg(
        vedleggId: VedleggId,
        idToken: IdToken,
        callId: CallId
    ) {
        k9MellomlagringGateway.slettVedlegg(
            vedleggId = vedleggId,
            idToken = idToken,
            callId = callId
        )
    }

    suspend fun slettVedlegg(
        vedleggUrls: List<URL>,
        idToken: IdToken,
        callId: CallId
    ) {
        coroutineScope {
            val futures = mutableListOf<Deferred<Unit>>()
            vedleggUrls.forEach {
                futures.add(async { slettVedlegg(
                    vedleggId = vedleggIdFromUrl(it),
                    idToken = idToken,
                    callId = callId
                )})

            }
            futures.awaitAll()
        }
    }

    suspend fun settPåHold(
        vedleggUrls: List<URL>,
        idToken: IdToken,
        callId: CallId
    ) {
        coroutineScope {
            val futures = mutableListOf<Deferred<Unit>>()
            vedleggUrls.forEach {
                futures.add(async { settPåHold(
                    vedleggId = vedleggIdFromUrl(it),
                    idToken = idToken,
                    callId = callId
                ) })
            }
            futures.awaitAll()
        }
    }

    suspend fun settPåHold(
        vedleggId: VedleggId,
        idToken: IdToken,
        callId: CallId
    ) {
        k9MellomlagringGateway.settPåHold(
            vedleggId = vedleggId,
            idToken = idToken,
            callId = callId
        )
    }

    private fun vedleggIdFromUrl(url: URL) : VedleggId {
        return VedleggId(url.path.substringAfterLast("/"))
    }
}
