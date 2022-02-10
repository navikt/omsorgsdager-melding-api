package no.nav.omsorgsdagermeldingapi.general.systemauth

import com.nimbusds.jose.jwk.JWK
import no.nav.helse.dusseldorf.ktor.auth.Client
import no.nav.helse.dusseldorf.ktor.auth.PrivateKeyClient
import no.nav.helse.dusseldorf.oauth2.client.DirectKeyId
import no.nav.helse.dusseldorf.oauth2.client.FromJwk
import no.nav.helse.dusseldorf.oauth2.client.SignedJwtAccessTokenClient

internal class AccessTokenClientResolver(
    private val clients: Map<String, Client>
) {
    companion object {
        private const val AZURE_V2_ALIAS = "azure-v2"
        private const val TOKEN_X_ALIAS = "tokenx"
    }

    internal val tokenxClient = createSignedJwtAccessTokenClient(resolveClient(TOKEN_X_ALIAS))
    internal val azureV2AccessTokenClient = createSignedJwtAccessTokenClient(resolveClient(AZURE_V2_ALIAS))

    private fun createSignedJwtAccessTokenClient(client: PrivateKeyClient) = SignedJwtAccessTokenClient(
        clientId = client.clientId(),
        tokenEndpoint = client.tokenEndpoint(),
        privateKeyProvider = FromJwk(client.privateKeyJwk),
        keyIdProvider = DirectKeyId(resolveKeyId(client))
    )

    private fun resolveClient(alias: String) =
        clients.getOrElse(alias) {
            throw IllegalStateException("Client[${alias}] må være satt opp.")
        } as PrivateKeyClient

    private fun resolveKeyId(client: PrivateKeyClient) = try {
        val jwk = JWK.parse(client.privateKeyJwk)
        requireNotNull(jwk.keyID) { "Private JWK inneholder ikke keyID." }
        jwk.keyID
    } catch (_: Throwable) {
        throw IllegalArgumentException("Private JWK på feil format.")
    }
}