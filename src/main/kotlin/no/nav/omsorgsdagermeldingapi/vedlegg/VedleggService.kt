package no.nav.omsorgsdagermeldingapi.vedlegg

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.helse.dusseldorf.ktor.auth.IdToken
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.søknad.melding.vedleggId
import java.net.URL

class VedleggService(
    private val k9MellomlagringGateway: K9MellomlagringGateway
) {

    suspend fun lagreVedlegg(
        vedlegg: Vedlegg,
        idToken: IdToken,
        callId: CallId
    ): String {

        return k9MellomlagringGateway.lagreVedlegg(
            vedlegg = vedlegg,
            idToken = idToken,
            callId = callId
        )

    }

    suspend fun slettVedlegg(
        vedleggId: String,
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
        k9MellomlagringGateway.slettPersistertVedlegg(
            vedleggId = vedleggsUrls.map { it.vedleggId() },
            callId = callId,
            eier = eier
        )
    }

    internal suspend fun persisterVedlegg(
        vedleggsUrls: List<URL>,
        callId: CallId,
        eier: DokumentEier
    ) {

        k9MellomlagringGateway.persisterVedlegger(
            vedleggId = vedleggsUrls.map { it.vedleggId() },
            callId = callId,
            eier = eier
        )
    }

    suspend fun hentVedlegg(
        vedleggId: String,
        idToken: IdToken,
        callId: CallId,
        eier: DokumentEier
    ): Vedlegg? {

        return k9MellomlagringGateway.hentVedlegg(
            vedleggId = vedleggId,
            idToken = idToken,
            eier = eier,
            callId = callId
        )
    }

    suspend fun hentVedlegg(
        idToken: IdToken,
        vedleggUrls: List<URL>,
        eier: DokumentEier,
        callId: CallId
    ): List<Vedlegg> {
        val vedlegg = coroutineScope {
            val futures = mutableListOf<Deferred<Vedlegg?>>()
            vedleggUrls.forEach {
                futures.add(async {
                    hentVedlegg(
                        vedleggId = it.vedleggId(),
                        idToken = idToken,
                        eier = eier,
                        callId = callId
                    )
                })

            }
            futures.awaitAll().filter { it != null }
        }
        return vedlegg.requireNoNulls()
    }
}
