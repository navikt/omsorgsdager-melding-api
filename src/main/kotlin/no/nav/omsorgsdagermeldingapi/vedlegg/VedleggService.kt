package no.nav.omsorgsdagermeldingapi.vedlegg

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

    suspend fun slettPersistertVedlegg(
        vedleggsUrls: List<URL>,
        callId: CallId,
        eier: DokumentEier
    ) {
        val vedleggsId = mutableListOf<VedleggId>()
        vedleggsUrls.forEach { vedleggsId.add(vedleggIdFromUrl(it)) }

        k9MellomlagringGateway.slettPersistertVedlegg(
            vedleggId = vedleggsId,
            callId = callId,
            eier = eier
        )
    }

    internal suspend fun persisterVedlegg(
        vedleggsUrls: List<URL>,
        callId: CallId,
        eier: DokumentEier
    ) {
        val vedleggsId = mutableListOf<VedleggId>()
        vedleggsUrls.forEach { vedleggsId.add(vedleggIdFromUrl(it)) }

        k9MellomlagringGateway.persisterVedlegger(
            vedleggId = vedleggsId,
            callId = callId,
            eier = eier
        )
    }

    private fun vedleggIdFromUrl(url: URL) : VedleggId {
        return VedleggId(url.path.substringAfterLast("/"))
    }
}