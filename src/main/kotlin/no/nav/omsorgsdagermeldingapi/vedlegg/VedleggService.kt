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

    suspend fun slettVedlegg(
        vedleggId: VedleggId,
        idToken: IdToken,
        callId: CallId,
        eier: DokumentEier
    ): Boolean {
        return k9MellomlagringGateway.slettVedlegg(
            vedleggId = vedleggId,
            idToken = idToken,
            callId = callId,
            eier = eier
        )
    }

    suspend fun settPåHold(
        vedleggUrls: List<URL>,
        callId: CallId,
        eier: DokumentEier
    ) {
        coroutineScope {
            val futures = mutableListOf<Deferred<Unit>>()
            vedleggUrls.forEach {
                futures.add(async { settPåHold(
                    vedleggId = vedleggIdFromUrl(it),
                    callId = callId,
                    eier = eier
                ) })
            }
            futures.awaitAll()
        }
    }

    suspend fun settPåHold(
        vedleggId: VedleggId,
        callId: CallId,
        eier: DokumentEier
    ){
        k9MellomlagringGateway.settPåHold(
            vedleggId = vedleggId,
            callId = callId,
            eier = eier
        )
    }

    private fun vedleggIdFromUrl(url: URL) : VedleggId {
        return VedleggId(url.path.substringAfterLast("/"))
    }
}
