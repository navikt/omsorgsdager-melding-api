package no.nav.omsorgsdagermeldingapi

import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.omsorgsdagermeldingapi.wiremock.omsorgspengerMidlertidigAleneApiConfig
import no.nav.omsorgsdagermeldingapi.wiremock.stubK9OppslagSoker
import no.nav.omsorgsdagermeldingapi.wiremock.stubOppslagHealth
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ApplicationWithMocks {
    companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationWithMocks::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val wireMockServer = WireMockBuilder()
                .withPort(8081)
                .withAzureSupport()
                .withNaisStsSupport()
                .withLoginServiceSupport()
                .omsorgspengerMidlertidigAleneApiConfig()
                .build()
                .stubOppslagHealth()
                .stubK9OppslagSoker()

            val testArgs = TestConfiguration.asMap(
                port = 8082,
                wireMockServer = wireMockServer
            ).asArguments()

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    wireMockServer.stop()
                    logger.info("Tear down complete")
                }
            })

            withApplication { no.nav.omsorgsdagermeldingapi.main(testArgs) }
        }
    }
}