package no.nav.omsorgsdagermeldingapi.søknad

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.runBlocking
import no.nav.helse.dusseldorf.ktor.auth.IdToken
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import no.nav.omsorgsdagermeldingapi.MeldingUtils
import no.nav.omsorgsdagermeldingapi.barn.Barn
import no.nav.omsorgsdagermeldingapi.barn.BarnService
import no.nav.omsorgsdagermeldingapi.felles.Metadata
import no.nav.omsorgsdagermeldingapi.general.CallId
import no.nav.omsorgsdagermeldingapi.kafka.SøknadKafkaProducer
import no.nav.omsorgsdagermeldingapi.søker.Søker
import no.nav.omsorgsdagermeldingapi.søker.SøkerService
import no.nav.omsorgsdagermeldingapi.vedlegg.DokumentEier
import no.nav.omsorgsdagermeldingapi.vedlegg.Vedlegg
import no.nav.omsorgsdagermeldingapi.vedlegg.VedleggService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.Test

internal class SøknadServiceTest{
    @RelaxedMockK
    lateinit var kafkaProducer: SøknadKafkaProducer

    @RelaxedMockK
    lateinit var søkerService: SøkerService

    @RelaxedMockK
    lateinit var barnService: BarnService

    @RelaxedMockK
    lateinit var vedleggService: VedleggService

    lateinit var søknadService: SøknadService

    @BeforeEach
    internal fun setUp() {
        MockKAnnotations.init(this)
        søknadService = SøknadService(
            søkerService = søkerService,
            kafkaProducer = kafkaProducer,
            vedleggService = vedleggService,
            barnService = barnService
        )
        assertNotNull(kafkaProducer)
        assertNotNull(søknadService)
    }

    @Test
    internal fun `Tester at den sletter persistert vedlegg dersom kafka feiler`() {
        assertThrows<MeldingRegistreringFeiletException> {
            runBlocking {
                coEvery {søkerService.getSøker(any(), any()) } returns Søker(
                    aktørId = "123",
                    fødselsdato = LocalDate.parse("2000-01-01"),
                    fødselsnummer = "290990123456"
                )

                coEvery {barnService.hentNåværendeBarn(any(), any()) } returns listOf(
                    Barn(
                        fødselsdato = LocalDate.now().minusDays(30),
                        identitetsnummer = "07127621904",
                        aktørId = "12345",
                        fornavn = "Jens",
                        etternavn = "Jensen",
                        mellomnavn = "Noe"
                    )
                )

                coEvery {vedleggService.hentVedlegg(idToken = any(), any(), any(), any()) } returns listOf(Vedlegg("bytearray".toByteArray(), "vedlegg", "vedlegg", DokumentEier("290990123456")))

                every { kafkaProducer.produserKafkaMelding(any(), any()) } throws Exception("Mocket feil ved kafkaProducer")

                søknadService.registrer(
                    melding = MeldingUtils.gyldigMeldingFordele,
                    metadata = Metadata(
                        version = 1,
                        correlationId = "123"
                    ),
                    idToken = IdToken(Azure.V2_0.generateJwt(clientId = "ikke-authorized-client", audience = "omsorgsdager-melding-api")),
                    callId = CallId("abc")
                )
            }
        }

        coVerify(exactly = 1) { vedleggService.slettPersistertVedlegg(any(), any(), any()) }
    }
}