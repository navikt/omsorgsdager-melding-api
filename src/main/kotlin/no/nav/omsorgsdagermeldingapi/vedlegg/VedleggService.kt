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

    internal suspend fun persisterVedleggApi( //Kun for å test med api, metoden under skal  brukes for meldinger
        vedleggId: VedleggId,
        callId: CallId,
        eier: DokumentEier
    ){
        k9MellomlagringGateway.persisterVedlegger(
            vedleggId = listOf(vedleggId),
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