package no.nav.omsorgsdagermeldingapi

import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.omsorgsdagermeldingapi.søknad.melding.*
import org.junit.Test
import java.net.URL
import java.time.LocalDate
import kotlin.test.assertTrue

internal class MeldingValideringsTest {

    //--- Felles ---
    @Test
    fun `Tester gyldig fødselsdato dersom dnunmer`() {
        val starterMedFodselsdato = "630293".starterMedFodselsdato()
        assertTrue(starterMedFodselsdato)
    }

    @Test
    fun `Gyldig søknad`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harForståttRettigheterOgPlikter er false`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harForståttRettigheterOgPlikter = false
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Feiler dersom harBekreftetOpplysninger er false`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harBekreftetOpplysninger = false
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom mottakerNavn er tom`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            mottakerNavn = " "
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom mottakerFnr er ugyldig`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            mottakerFnr = "ikke gyldig fnr"
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom harAleneomsorg er null`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harAleneomsorg = null
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom harUtvidetRett er null`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harUtvidetRett = null
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom erYrkesaktiv er null`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            erYrkesaktiv = null
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom arbeiderINorge er null`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            arbeiderINorge = null
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom arbeidssituasjon er tom`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            arbeidssituasjon = listOf()
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom barn er en tom liste`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            barn = listOf()
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom barn ikke har identitetsnummer`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            barn = listOf(
                BarnUtvidet(
                    navn = "Kjell",
                    identitetsnummer = null,
                    aleneOmOmsorgen = true,
                    utvidetRett = true,
                    fødselsdato = LocalDate.parse("2020-01-01"),
                    aktørId = "1000000000001"
                )
            )
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom barn identitetsnummer er ugyldig`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            barn = listOf(
                BarnUtvidet(
                    navn = "Kjell",
                    identitetsnummer = "ikke gyldig",
                    aleneOmOmsorgen = true,
                    utvidetRett = true,
                    fødselsdato = LocalDate.parse("2020-01-01"),
                    aktørId = "1000000000001"
                )
            )
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom barn navn er ugyldig`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            barn = listOf(
                BarnUtvidet(
                    navn = " ",
                    identitetsnummer = "16012099359",
                    aleneOmOmsorgen = true,
                    utvidetRett = true,
                    fødselsdato = LocalDate.parse("2020-01-01"),
                    aktørId = "1000000000001"
                )
            )
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom barn aleneOmOmsorgen er null`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            barn = listOf(
                BarnUtvidet(
                    navn = "Kjell",
                    identitetsnummer = "16012099359",
                    aleneOmOmsorgen = null,
                    utvidetRett = true,
                    fødselsdato = LocalDate.parse("2020-01-01"),
                    aktørId = "1000000000001"
                )
            )
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile dersom barn utvidetRett er null`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            barn = listOf(
                BarnUtvidet(
                    navn = "Kjell",
                    identitetsnummer = "16012099359",
                    aleneOmOmsorgen = true,
                    utvidetRett = null,
                    fødselsdato = LocalDate.parse("2020-01-01"),
                    aktørId = "1000000000001"
                )
            )
        )
        melding.valider()
    }

    //--- Koronaoverføring
    @Test
    fun `Skal ikke feile på gyldig koronaoverføring`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile type er KORONA_OVERFØRE men korona er null`(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            type = Meldingstype.KORONA_OVERFØRE,
            korona = null
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile type er KORONA_OVERFØRE men antallDagerSomSkalOverføres er under MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE `(){
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            type = Meldingstype.KORONA_OVERFØRE,
            korona = Koronaoverføre(
                antallDagerSomSkalOverføres = 0
            )
        )
        melding.valider()
    }

    //--- Overføring
    @Test
    fun `Skal ikke feile på gyldig overføring`(){
        val melding = MeldingUtils.gyldigMeldingOverføre
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile hvis type er OVERFØRE men overføring er null`(){
        val melding = MeldingUtils.gyldigMeldingOverføre.copy(
            type = Meldingstype.OVERFØRE,
            overføring = null
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile hvis type er OVERFØRE men antallDagerSomSkalOverføres er under MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE `(){
        val melding = MeldingUtils.gyldigMeldingOverføre.copy(
            type = Meldingstype.OVERFØRE,
            overføring = Overføre(
                mottakerType = Mottaker.SAMBOER,
                antallDagerSomSkalOverføres = 0
            )
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile hvis type er OVERFØRE men mottakerType er samværsforelder `(){
        val melding = MeldingUtils.gyldigMeldingOverføre.copy(
            type = Meldingstype.OVERFØRE,
            overføring = Overføre(
                mottakerType = Mottaker.SAMVÆRSFORELDER,
                antallDagerSomSkalOverføres = 1
            )
        )
        melding.valider()
    }

    //--- Fordeling
    @Test
    fun `Skal ikke feile på gyldig fordeling`(){
        val melding = MeldingUtils.gyldigMeldingFordele
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile hvis type er FORDELE men fordeling er null`(){
        val melding = MeldingUtils.gyldigMeldingFordele.copy(
            type = Meldingstype.FORDELE,
            fordeling = null
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile hvis type er FORDELE men mottakerType ikke er samværsforelder`(){
        val melding = MeldingUtils.gyldigMeldingFordele.copy(
            type = Meldingstype.FORDELE,
            fordeling = Fordele(
                mottakerType = Mottaker.SAMBOER,
                samværsavtale = listOf(URL("http://localhost:8080/vedlegg/1"))
            )
        )
        melding.valider()
    }

    @Test(expected = Throwblem::class)
    fun `Skal feile hvis type er FORDELE men samværsavtale er tom`(){
        val melding = MeldingUtils.gyldigMeldingFordele.copy(
            type = Meldingstype.FORDELE,
            fordeling = Fordele(
                mottakerType = Mottaker.SAMVÆRSFORELDER,
                samværsavtale = listOf()
            )
        )
        melding.valider()
    }

}