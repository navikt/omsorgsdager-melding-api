package no.nav.omsorgsdagermeldingapi

import com.github.fppt.jedismock.RedisServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.impl.annotations.InjectMockKs
import no.nav.common.KafkaEnvironment
import no.nav.helse.TestUtils.issueToken
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.omsorgsdagermeldingapi.felles.*
import no.nav.omsorgsdagermeldingapi.kafka.Topics
import no.nav.omsorgsdagermeldingapi.mellomlagring.started
import no.nav.omsorgsdagermeldingapi.søknad.melding.BarnUtvidet
import no.nav.omsorgsdagermeldingapi.søknad.melding.Fordele
import no.nav.omsorgsdagermeldingapi.søknad.melding.Mottaker
import no.nav.omsorgsdagermeldingapi.søknad.melding.vedleggId
import no.nav.omsorgsdagermeldingapi.wiremock.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.LocalDate
import java.util.*
import kotlin.test.*

@InjectMockKs
class ApplicationTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationTest::class.java)

        val redisServer: RedisServer = RedisServer
            .newRedisServer().started()
        val mockOAuth2Server = MockOAuth2Server().apply { start() }
        val wireMockServer = WireMockBuilder()
            .withAzureSupport()
            .withNaisStsSupport()
            .withLoginServiceSupport()
            .withTokendingsSupport()
            .omsorgsdagerMeldingApiConfig()
            .build()
            .stubOppslagHealth()
            .stubK9OppslagSoker()
            .stubK9OppslagBarn()
            .stubK9Mellomlagring()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestConsumer = kafkaEnvironment.testConsumer()

        private const val gyldigFodselsnummerA = "290990123456"
        private const val gyldigFodselsnummerB = "25118921464"
        private const val ikkeMyndigFnr = "12125012345"

        private val cookie = mockOAuth2Server.issueToken(
            fnr = gyldigFodselsnummerA,
            issuerId = "login-service-v2",
            somCookie = true
        )

        private val tokenXToken = mockOAuth2Server.issueToken(
            fnr = gyldigFodselsnummerA,
            issuerId = "tokendings"
        )

        private val tokenXTokenB = mockOAuth2Server.issueToken(
            fnr = gyldigFodselsnummerB,
            issuerId = "tokendings"
        )

        private val ikkeMyndigTokenX = mockOAuth2Server.issueToken(
            fnr = ikkeMyndigFnr,
            issuerId = "tokendings"
        )

        fun getConfig(kafkaEnvironment: KafkaEnvironment?): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    redisServer = redisServer,
                    kafkaEnvironment = kafkaEnvironment,
                    mockOAuth2Server = mockOAuth2Server
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)

            return HoconApplicationConfig(mergedConfig)
        }


        var engine = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaEnvironment)
        })


        @BeforeAll
        @JvmStatic
        fun buildUp() {
            engine.start(wait = true)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            redisServer.stop()
            mockOAuth2Server.shutdown()
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
    fun `Tester lagring og sletting av vedlegg`() {
        val jpeg = "vedlegg/iPhone_6.jpg".fromResources().readBytes()

        with(engine) {
            // LASTER OPP VEDLEGG
            val url = handleRequestUploadImage(
                cookie = cookie,
                vedlegg = jpeg
            )
            val path = Url(url).fullPath
            // SLETTER OPPLASTET VEDLEGG
            handleRequest(HttpMethod.Delete, path) {
                addHeader("Cookie", cookie)
            }.apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun `Tester lagring og sletting av vedlegg med tokenx`() {
        val jpeg = "vedlegg/iPhone_6.jpg".fromResources().readBytes()

        with(engine) {
            // LASTER OPP VEDLEGG
            val url = handleRequestUploadImage(
                vedlegg = jpeg,
                jwtToken = tokenXToken
            )
            val path = Url(url).fullPath
            // SLETTER OPPLASTET VEDLEGG
            handleRequest(HttpMethod.Delete, path) {
                addHeader("Authorization", "Bearer $tokenXToken")
            }.apply {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun `Tester sletting av vedlegg som ikke finnes`(){
        with(engine){
            handleRequest(HttpMethod.Delete, "/vedlegg/123") {
                addHeader("Cookie", tokenXToken)
            }.apply {
                assertNotEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test
    fun `Hente søker med tokenx`() {
        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = SØKER_URL,
            expectedCode = HttpStatusCode.OK,
            jwtToken = tokenXToken,
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
    fun `Hente søker`() {
        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = SØKER_URL,
            expectedCode = HttpStatusCode.OK,
            cookie = cookie,
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
            jwtToken = ikkeMyndigTokenX,
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
    fun `Hente søker hvor man får 451 fra oppslag`() {
        wireMockServer.stubK9OppslagSoker(
            statusCode = HttpStatusCode.fromValue(451),
            responseBody =
            //language=json
            """
            {
                "detail": "Policy decision: DENY - Reason: (NAV-bruker er i live AND NAV-bruker er ikke myndig)",
                "instance": "/meg",
                "type": "/problem-details/tilgangskontroll-feil",
                "title": "tilgangskontroll-feil",
                "status": 451
            }
            """.trimIndent()
        )

        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = SØKER_URL,
            expectedCode = HttpStatusCode.fromValue(451),
            expectedResponse =
            //language=json
            """
            {
                "type": "/problem-details/tilgangskontroll-feil",
                "title": "tilgangskontroll-feil",
                "status": 451,
                "instance": "/soker",
                "detail": "Tilgang nektet."
            }
            """.trimIndent(),
            cookie = cookie
        )

        wireMockServer.stubK9OppslagSoker()
    }

    @Test
    fun `Hente barn og sjekk eksplisit at identitetsnummer ikke blir med ved get kall`(){

        val respons = requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = "/barn",
            expectedCode = HttpStatusCode.OK,
            cookie = cookie,
            //language=json
            expectedResponse = """
                {
                  "barn": [
                    {
                      "fødselsdato": "2000-08-27",
                      "fornavn": "BARN",
                      "mellomnavn": "EN",
                      "etternavn": "BARNESEN",
                      "aktørId": "1000000000001"
                    },
                    {
                      "fødselsdato": "2001-04-10",
                      "fornavn": "BARN",
                      "mellomnavn": "TO",
                      "etternavn": "BARNESEN",
                      "aktørId": "1000000000002"
                    }
                  ]
                }
            """.trimIndent()
        )

        val responsSomJSONArray = JSONObject(respons).getJSONArray("barn")

        assertFalse(responsSomJSONArray.getJSONObject(0).has("identitetsnummer"))
        assertFalse(responsSomJSONArray.getJSONObject(1).has("identitetsnummer"))
    }

    @Test
    fun `Feil ved henting av barn skal returnere tom liste`() {
        wireMockServer.stubK9OppslagBarn(simulerFeil = true)
        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = BARN_URL,
            expectedCode = HttpStatusCode.OK,
            expectedResponse = """
            {
                "barn": []
            }
            """.trimIndent(),
            jwtToken = tokenXTokenB
        )
        wireMockServer.stubK9OppslagBarn()
    }

    @Test
    fun `Hente barn hvor man får 451 fra oppslag`(){
        wireMockServer.stubK9OppslagBarn(
            statusCode = HttpStatusCode.fromValue(451),
            responseBody =
            //language=json
            """
            {
                "detail": "Policy decision: DENY - Reason: (NAV-bruker er i live AND NAV-bruker er ikke myndig)",
                "instance": "/meg",
                "type": "/problem-details/tilgangskontroll-feil",
                "title": "tilgangskontroll-feil",
                "status": 451
            }
            """.trimIndent()
        )

        requestAndAssert(
            httpMethod = HttpMethod.Get,
            path = BARN_URL,
            expectedCode = HttpStatusCode.fromValue(451),
            expectedResponse =
            //language=json
            """
            {
                "type": "/problem-details/tilgangskontroll-feil",
                "title": "tilgangskontroll-feil",
                "status": 451,
                "instance": "/barn",
                "detail": "Tilgang nektet."
            }
            """.trimIndent(),
            cookie = cookie
        )

        wireMockServer.stubK9OppslagBarn() // reset til default mapping
    }

    //Koronaoverføring
    @Test
    fun `Sende gyldig melding om koronaoverføring av omsorgsdager og plukke opp fra kafka topic`() {
        val søknadID = UUID.randomUUID().toString()
        val søknad = MeldingUtils.gyldigMeldingKoronaoverføre.copy(søknadId = søknadID).somJson()

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = MELDING_URL_KORONAOVERFØRE,
            expectedResponse = null,
            expectedCode = HttpStatusCode.Accepted,
            cookie = cookie,
            requestEntity = søknad
        )

        val søknadSendtTilProsessering = hentMeldingSendtTilProsessering(søknadID)
        verifiserAtInnholdetErLikt(JSONObject(søknad), søknadSendtTilProsessering)
    }

    @Test
    fun `Sende søknad hvor søker ikke er myndig`() {
        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = MELDING_URL_KORONAOVERFØRE,
            expectedResponse = """
                {
                    "type": "/problem-details/unauthorized",
                    "title": "unauthorized",
                    "status": 403,
                    "detail": "Søkeren er ikke myndig og kan ikke sende inn melding.",
                    "instance": "about:blank"
                }
            """.trimIndent(),
            expectedCode = HttpStatusCode.Forbidden,
            jwtToken= ikkeMyndigTokenX,
            requestEntity = MeldingUtils.gyldigMeldingKoronaoverføre.somJson()
        )
    }

    //Overføring
    @Test
    fun `Sende gyldig melding om overføring av omsorgsdager og plukke opp fra kafka topic`() {
        val søknadID = UUID.randomUUID().toString()
        val søknad = MeldingUtils.gyldigMeldingOverføre.copy(søknadId = søknadID).somJson()

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = MELDING_URL_OVERFØRE,
            cookie = cookie,
            expectedResponse = null,
            expectedCode = HttpStatusCode.Accepted,
            requestEntity = søknad
        )

        val søknadSendtTilProsessering = hentMeldingSendtTilProsessering(søknadID)
        verifiserAtInnholdetErLikt(JSONObject(søknad), søknadSendtTilProsessering)
    }

    @Test
    fun `Teste at identitetsnummer blir satt på barn etter innsending`(){
        val søknadID = UUID.randomUUID().toString()
        val søknad = MeldingUtils.gyldigMeldingOverføre.copy(
            søknadId = søknadID,
            barn = listOf(
                BarnUtvidet(
                    navn = "Kjell",
                    aleneOmOmsorgen = true,
                    utvidetRett = true,
                    fødselsdato = LocalDate.parse("2020-01-01"),
                    aktørId = "1000000000001"
                )
            )
        ).somJson()

        requestAndAssert(
            httpMethod = HttpMethod.Post,
            path = MELDING_URL_OVERFØRE,
            expectedResponse = null,
            expectedCode = HttpStatusCode.Accepted,
            requestEntity = søknad,
            cookie = cookie
        )

        val meldingSendtTilProsessering = hentMeldingSendtTilProsessering(søknadID)
        val barn = meldingSendtTilProsessering.getJSONArray("barn").getJSONObject(0)
        JSONAssert.assertEquals(barn.getString("identitetsnummer"), "16012099359", true)
    }

    //Fordeling
    @Test
    fun `Sende gyldig melding om fordeling av omsorgsdager og plukke opp fra kafka topic`() {
        with(engine) {
            val søknadID = UUID.randomUUID().toString()
            val jpeg = "vedlegg/iPhone_6.jpg".fromResources().readBytes()

            // LASTER OPP VEDLEGG
            val vedleggId = handleRequestUploadImage(
                cookie = cookie,
                vedlegg = jpeg
            ).substringAfterLast("/")

            val søknad = MeldingUtils.gyldigMeldingFordele.copy(
                søknadId = søknadID,
                fordeling = Fordele(
                        Mottaker.SAMVÆRSFORELDER,
                        samværsavtale = listOf(URL("${wireMockServer.getK9MellomlagringUrl()}/$vedleggId"))
                )
            ).somJson()


            requestAndAssert(
                httpMethod = HttpMethod.Post,
                path = MELDING_URL_FORDELE,
                expectedResponse = null,
                cookie = cookie,
                expectedCode = HttpStatusCode.Accepted,
                requestEntity = søknad
            )


            val søknadSendtTilProsessering = hentMeldingSendtTilProsessering(søknadID)
            verifiserAtInnholdetErLikt(JSONObject(søknad), søknadSendtTilProsessering)
        }
    }

    @Test
    fun `Sende gyldig melding om fordeling av omsorgsdager med tokenx og plukke opp fra kafka topic`() {
        with(engine) {
            val søknadID = UUID.randomUUID().toString()
            val jpeg = "vedlegg/iPhone_6.jpg".fromResources().readBytes()

            // LASTER OPP VEDLEGG
            val vedleggId = handleRequestUploadImage(
                jwtToken = tokenXToken,
                vedlegg = jpeg
            ).substringAfterLast("/")

            val søknad = MeldingUtils.gyldigMeldingFordele.copy(
                søknadId = søknadID,
                fordeling = Fordele(
                        Mottaker.SAMVÆRSFORELDER,
                        samværsavtale = listOf(URL("${wireMockServer.getK9MellomlagringUrl()}/$vedleggId"))
                )
            ).somJson()


            requestAndAssert(
                httpMethod = HttpMethod.Post,
                path = MELDING_URL_FORDELE,
                expectedResponse = null,
                jwtToken = tokenXToken,
                expectedCode = HttpStatusCode.Accepted,
                requestEntity = søknad
            )


            val søknadSendtTilProsessering = hentMeldingSendtTilProsessering(søknadID)
            verifiserAtInnholdetErLikt(JSONObject(søknad), søknadSendtTilProsessering)
        }
    }

    private fun requestAndAssert(
        httpMethod: HttpMethod,
        path: String,
        requestEntity: String? = null,
        expectedResponse: String?,
        expectedCode: HttpStatusCode,
        jwtToken: String? = null,
        cookie: String? = null
    ): String? {
        val respons: String?
        with(engine) {
            handleRequest(httpMethod, path) {
                if (cookie != null) addHeader(HttpHeaders.Cookie, cookie)
                if (jwtToken != null) addHeader(HttpHeaders.Authorization, "Bearer $jwtToken")
                logger.info("Request Entity = $requestEntity")
                addHeader(HttpHeaders.Accept, "application/json")
                if (requestEntity != null) addHeader(HttpHeaders.ContentType, "application/json")
                if (requestEntity != null) setBody(requestEntity)
            }.apply {
                logger.info("Response Entity = ${response.content}")
                logger.info("Expected Entity = $expectedResponse")
                respons = response.content
                assertEquals(expectedCode, response.status())
                if (expectedResponse != null) {
                    JSONAssert.assertEquals(expectedResponse, response.content!!, true)
                } else {
                    assertEquals(expectedResponse, response.content)
                }
            }
        }
        return respons
    }

    private fun hentMeldingSendtTilProsessering(soknadId: String): JSONObject {
        return kafkaTestConsumer.hentMelding(soknadId, topic = Topics.MOTTATT_OMSORGSDAGER_MELDING).data
    }

    private fun verifiserAtInnholdetErLikt(
        søknadSendtInn: JSONObject,
        søknadPlukketFraTopic: JSONObject
    ) {
        assertTrue(søknadPlukketFraTopic.has("søker"))
        søknadPlukketFraTopic.remove("søker") //Fjerner søker siden det settes i komplettSøknad

        assertTrue(søknadPlukketFraTopic.has("mottatt"))
        søknadPlukketFraTopic.remove("mottatt") //Fjerner mottatt siden det settes i komplettSøknad

        if(søknadSendtInn.get("fordeling").toString() != "null"){
            val vedleggUrl = søknadSendtInn.getJSONObject("fordeling").getJSONArray("samværsavtale").map { it.toString() }
            val forventetVedleggId = vedleggUrl.map { it.vedleggId() }
            val faktiskVedleggId = søknadPlukketFraTopic.getJSONObject("fordeling").getJSONArray("samværsavtaleVedleggId").map { it.toString() }

            assertEquals(forventetVedleggId, faktiskVedleggId)

            søknadSendtInn.getJSONObject("fordeling").remove("samværsavtale")
            søknadPlukketFraTopic.getJSONObject("fordeling").remove("samværsavtaleVedleggId")
        }

        JSONAssert.assertEquals(søknadSendtInn, søknadPlukketFraTopic, true)
    }
}