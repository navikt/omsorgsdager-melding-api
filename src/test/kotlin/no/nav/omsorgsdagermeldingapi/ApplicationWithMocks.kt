package no.nav.omsorgsdagermeldingapi

import com.github.fppt.jedismock.RedisServer
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.omsorgsdagermeldingapi.mellomlagring.started
import no.nav.omsorgsdagermeldingapi.wiremock.omsorgsdagerMeldingApiConfig
import no.nav.omsorgsdagermeldingapi.wiremock.stubK9OppslagBarn
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
                .omsorgsdagerMeldingApiConfig()
                .build()
                .stubOppslagHealth()
                .stubK9OppslagSoker()
                .stubK9OppslagBarn()

            val redisServer: RedisServer = RedisServer
                .newRedisServer(6379)
                .started()

            val testArgs = TestConfiguration.asMap(
                wireMockServer = wireMockServer,
                port = 8082,
                redisServer = redisServer
            ).asArguments()

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    wireMockServer.stop()
                    redisServer.stop()
                    logger.info("Tear down complete")
                }
            })

            withApplication { no.nav.omsorgsdagermeldingapi.main(testArgs) }
        }
    }
}
