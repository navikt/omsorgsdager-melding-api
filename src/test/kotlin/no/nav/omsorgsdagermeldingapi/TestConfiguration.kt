package no.nav.omsorgsdagermeldingapi

import com.github.fppt.jedismock.RedisServer
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getTokendingsWellKnownUrl
import no.nav.omsorgsdagermeldingapi.wiremock.getK9MellomlagringUrl
import no.nav.omsorgsdagermeldingapi.wiremock.getK9OppslagUrl
import no.nav.security.mock.oauth2.MockOAuth2Server

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        kafkaEnvironment: KafkaEnvironment? = null,
        port: Int = 8080,
        k9OppslagUrl: String? = wireMockServer?.getK9OppslagUrl(),
        k9MellomlagringUrl: String? = wireMockServer?.getK9MellomlagringUrl(),
        corsAdresses: String = "http://localhost:8080",
        redisServer: RedisServer,
        mockOAuth2Server: MockOAuth2Server
    ) : Map<String, String> {

        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),
            Pair("nav.authorization.cookie_name", "selvbetjening-idtoken"),
            Pair("nav.gateways.k9_oppslag_url","$k9OppslagUrl"),
            Pair("nav.gateways.k9_mellomlagring_url","$k9MellomlagringUrl"),
            Pair("nav.cors.addresses", corsAdresses)
        )

        if (wireMockServer != null) {
            // Clients
            map["nav.auth.clients.0.alias"] = "azure-v2"
            map["nav.auth.clients.0.client_id"] = "omsorgsdager-melding-api"
            map["nav.auth.clients.0.private_key_jwk"] = ClientCredentials.ClientC.privateKeyJwk
            map["nav.auth.clients.0.certificate_hex_thumbprint"] = "The keyId of Azure JWK"
            map["nav.auth.clients.0.discovery_endpoint"] = wireMockServer.getAzureV2WellKnownUrl()

            map["nav.auth.clients.1.alias"] = "tokenx"
            map["nav.auth.clients.1.client_id"] = "omsorgsdager-melding-api"
            map["nav.auth.clients.1.private_key_jwk"] = ClientCredentials.ClientC.privateKeyJwk
            map["nav.auth.clients.1.discovery_endpoint"] = wireMockServer.getTokendingsWellKnownUrl()

            // Issuers
            map["no.nav.security.jwt.issuers.0.issuer_name"] = "tokendings"
            map["no.nav.security.jwt.issuers.0.discoveryurl"] = "${mockOAuth2Server.wellKnownUrl("tokendings")}"
            map["no.nav.security.jwt.issuers.0.accepted_audience"] = "dev-gcp:dusseldorf:omsorgsdager-melding-api"

            map["no.nav.security.jwt.issuers.1.issuer_name"] = "login-service"
            map["no.nav.security.jwt.issuers.1.discoveryurl"] = "${mockOAuth2Server.wellKnownUrl("login-service-v2")}"
            map["no.nav.security.jwt.issuers.1.accepted_audience"] = "dev-gcp:dusseldorf:omsorgsdager-melding-api"
            map["no.nav.security.jwt.issuers.1.cookie_name"] = "selvbetjening-idtoken"

            // Scopes
            map["nav.auth.scopes.persistere-dokument"] = "k9-mellomlagring/.default"
            map["nav.auth.scopes.k9_mellomlagring_tokenx_audience"] = "dev-gcp:dusseldorf:k9-mellomlagring"
            map["nav.auth.scopes.k9_selvbetjening_oppslag_tokenx_audience"] = "dev-fss:dusseldorf:k9-selvbetjening-oppslag"
        }

        // Kafka
        kafkaEnvironment?.let {
            map["nav.kafka.bootstrap_servers"] = it.brokersURL
        }

        map["nav.redis.host"] = "localhost"
        map["nav.redis.port"] = "${redisServer.bindPort}"
        map["nav.storage.passphrase"] = "verySecret"

        return map.toMap()
    }

}
