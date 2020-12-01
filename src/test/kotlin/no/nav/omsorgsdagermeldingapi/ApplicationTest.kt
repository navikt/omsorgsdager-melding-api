package no.nav.omsorgsdagermeldingapi

import com.github.tomakehurst.wiremock.http.Cookie
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.getAuthCookie
import no.nav.omsorgsdagermeldingapi.felles.SØKER_URL
import no.nav.omsorgsdagermeldingapi.felles.SØKNAD_URL
import no.nav.omsorgsdagermeldingapi.felles.VALIDERING_URL
import no.nav.omsorgsdagermeldingapi.felles.somJson
import no.nav.omsorgsdagermeldingapi.kafka.Topics
import no.nav.omsorgsdagermeldingapi.redis.RedisMockUtil
import no.nav.omsorgsdagermeldingapi.wiremock.omsorgsdagerMeldingApiConfig
import no.nav.omsorgsdagermeldingapi.wiremock.stubK9OppslagSoker
import no.nav.omsorgsdagermeldingapi.wiremock.stubOppslagHealth
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val gyldigFodselsnummerA = "290990123456"
private const val ikkeMyndigFnr = "12125012345"

@KtorExperimentalAPI
class ApplicationTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationTest::class.java)

        val wireMockServer = WireMockBuilder()
            .withAzureSupport()
            .withNaisStsSupport()
            .withLoginServiceSupport()
            .omsorgsdagerMeldingApiConfig()
            .build()
            .stubOppslagHealth()
            .stubK9OppslagSoker()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestConsumer = kafkaEnvironment.testConsumer()

        fun getConfig(kafkaEnvironment: KafkaEnvironment): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    kafkaEnvironment = kafkaEnvironment
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)

            return HoconApplicationConfig(mergedConfig)
        }


        val engine = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaEnvironment)
        })


        @BeforeClass
        @JvmStatic
        fun buildUp() {
            engine.start(wait = true)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            RedisMockUtil.stopRedisMocked()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        handleRequest(HttpMethod.Get, "/health") {}.apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `Hente søker`() {
        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = SØKER_URL,
            expectedCode = HttpStatusCode.OK,
            expectedResponse = """
                {
                  "aktørId": "12345",
                  "fødselsdato": "1997-05-25",
                  "fødselsnummer": "290990123456",
                  "fornavn": "MOR",
                  "mellomnavn": "HEISANN",
                  "etternavn": "MORSEN",
                  "myndig": true
                }
            """.trimIndent()
        )
    }

    @Test
    fun `Hente søker som ikke er myndig`() {
        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = SØKER_URL,
            expectedCode = HttpStatusCode.OK,
            cookie = getAuthCookie(ikkeMyndigFnr),
            expectedResponse = """
                {
                  "aktørId": "12345",
                  "fødselsdato": "2050-12-12",
                  "fødselsnummer": "12125012345",
                  "fornavn": "MOR",
                  "mellomnavn": "HEISANN",
                  "etternavn": "MORSEN",
                  "myndig": false
                }
            """.trimIndent()
        )
    }

    @Test
    fun `Sende gyldig melding til validering`(){
        val søknad = SøknadUtils.gyldigSøknad.somJson()

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = SØKNAD_URL,
            expectedResponse = null,
            expectedCode = HttpStatusCode.Accepted,
            requestEntity = søknad
        )
    }

    @Test
    fun `Sende gyldig søknad og plukke opp fra kafka topic`() {
        val søknadID = UUID.randomUUID().toString()
        val søknad = SøknadUtils.gyldigSøknad.copy(søknadId = søknadID).somJson()

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = SØKNAD_URL,
            expectedResponse = null,
            expectedCode = HttpStatusCode.Accepted,
            requestEntity = søknad
        )

        val søknadSendtTilProsessering = hentSøknadSendtTilProsessering(søknadID)
        verifiserAtInnholdetErLikt(JSONObject(søknad), søknadSendtTilProsessering)
    }

    @Test
    fun `Sende søknad hvor søker ikke er myndig`() {
        val cookie = getAuthCookie(ikkeMyndigFnr)

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = SØKNAD_URL,
            expectedResponse = """
                {
                    "type": "/problem-details/unauthorized",
                    "title": "unauthorized",
                    "status": 403,
                    "detail": "Søkeren er ikke myndig og kan ikke sende inn søknaden.",
                    "instance": "about:blank"
                }
            """.trimIndent(),
            expectedCode = HttpStatusCode.Forbidden,
            cookie = cookie,
            requestEntity = SøknadUtils.gyldigSøknad.somJson()
        )
    }

    private fun requestAndAssert(
        httpMethod: HttpMethod,
        path: String,
        requestEntity: String? = null,
        expectedResponse: String?,
        expectedCode: HttpStatusCode,
        leggTilCookie: Boolean = true,
        cookie: Cookie = getAuthCookie(gyldigFodselsnummerA)
    ) {
        with(engine) {
            handleRequest(httpMethod, path) {
                if (leggTilCookie) addHeader(HttpHeaders.Cookie, cookie.toString())
                logger.info("Request Entity = $requestEntity")
                addHeader(HttpHeaders.Accept, "application/json")
                if (requestEntity != null) addHeader(HttpHeaders.ContentType, "application/json")
                if (requestEntity != null) setBody(requestEntity)
            }.apply {
                logger.info("Response Entity = ${response.content}")
                logger.info("Expected Entity = $expectedResponse")
                assertEquals(expectedCode, response.status())
                if (expectedResponse != null) {
                    JSONAssert.assertEquals(expectedResponse, response.content!!, true)
                } else {
                    assertEquals(expectedResponse, response.content)
                }
            }
        }
    }

    private fun hentSøknadSendtTilProsessering(soknadId: String): JSONObject {
        return kafkaTestConsumer.hentSøknad(soknadId, topic = Topics.MOTTATT_OMSORGSDAGER_MELDING).data
    }

    private fun verifiserAtInnholdetErLikt(
        søknadSendtInn: JSONObject,
        søknadPlukketFraTopic: JSONObject
    ) {
        assertTrue(søknadPlukketFraTopic.has("søker"))
        søknadPlukketFraTopic.remove("søker") //Fjerner søker siden det settes i komplettSøknad

        assertTrue(søknadPlukketFraTopic.has("mottatt"))
        søknadPlukketFraTopic.remove("mottatt") //Fjerner mottatt siden det settes i komplettSøknad

        JSONAssert.assertEquals(søknadSendtInn, søknadPlukketFraTopic, true)

        logger.info("Verifisering OK")
    }
}
