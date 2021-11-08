package no.nav.omsorgsdagermeldingapi

import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.omsorgsdagermeldingapi.søknad.melding.*
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.time.LocalDate
import kotlin.test.Test
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

    @Test
    fun `Feiler dersom harForståttRettigheterOgPlikter er false`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harForståttRettigheterOgPlikter = false
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }


    @Test
    fun `Feiler dersom harBekreftetOpplysninger er false`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harBekreftetOpplysninger = false
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }


    @Test
    fun `Skal feile dersom mottakerNavn er tom`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            mottakerNavn = " "
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom mottakerFnr er ugyldig`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            mottakerFnr = "ikke gyldig fnr"
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom harAleneomsorg er null`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harAleneomsorg = null
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom harUtvidetRett er null`() {

        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            harUtvidetRett = null
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom erYrkesaktiv er null`() {

        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            erYrkesaktiv = null
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom arbeiderINorge er null`() {

        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            arbeiderINorge = null
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom arbeidssituasjon er tom`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            arbeidssituasjon = listOf()
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom barn er en tom liste`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            barn = listOf()
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom barn ikke har identitetsnummer`() {
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
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom barn identitetsnummer er ugyldig`() {
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
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom barn navn er ugyldig`() {
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
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom barn aleneOmOmsorgen er null`() {
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
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile dersom barn utvidetRett er null`() {
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
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    //--- Koronaoverføring
    @Test
    fun `Skal ikke feile på gyldig koronaoverføring`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre
        melding.valider()
    }

    @Test
    fun `Skal feile hvis type er KORONA_OVERFØRE men korona er null`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            type = Meldingstype.KORONA,
            korona = null
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile hvis type er KORONA_OVERFØRE og man ønsker å overføre 1000 dager`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            type = Meldingstype.KORONA,
            korona = Koronaoverføre(
                antallDagerSomSkalOverføres = 1000,
                stengingsperiode = KoronaStengingsperiode(
                    fraOgMed = LocalDate.parse("2021-01-01"),
                    tilOgMed = LocalDate.parse("2021-12-31")
                )
            )
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile hvis type er KORONA_OVERFØRE og man ønsker å overføre 0 dager`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            type = Meldingstype.KORONA,
            korona = Koronaoverføre(
                antallDagerSomSkalOverføres = 0,
                stengingsperiode = KoronaStengingsperiode(
                    fraOgMed = LocalDate.parse("2021-01-01"),
                    tilOgMed = LocalDate.parse("2021-12-31")
                )
            )
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile type er KORONA_OVERFØRE men antallDagerSomSkalOverføres er under MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE `() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            type = Meldingstype.KORONA,
            korona = Koronaoverføre(
                antallDagerSomSkalOverføres = 0,
                stengingsperiode = KoronaStengingsperiode(
                    fraOgMed = LocalDate.parse("2021-01-01"),
                    tilOgMed = LocalDate.parse("2021-12-31")
                )
            )
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile type er KORONA_OVERFØRE men stengingsperiode er ikke kjent periode`() {
        val melding = MeldingUtils.gyldigMeldingKoronaoverføre.copy(
            type = Meldingstype.KORONA,
            korona = Koronaoverføre(
                antallDagerSomSkalOverføres = 0,
                stengingsperiode = KoronaStengingsperiode(
                    fraOgMed = LocalDate.parse("2021-01-01"),
                    tilOgMed = LocalDate.parse("2021-06-01")
                )
            )
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    //--- Overføring
    @Test
    fun `Skal ikke feile på gyldig overføring`() {
        val melding = MeldingUtils.gyldigMeldingOverføre
        melding.valider()
    }

    @Test
    fun `Skal feile hvis type er OVERFØRE men overføring er null`() {
        val melding = MeldingUtils.gyldigMeldingOverføre.copy(
            type = Meldingstype.OVERFORING,
            overføring = null
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile hvis type er OVERFØRE men antallDagerSomSkalOverføres er under MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE `() {
        val melding = MeldingUtils.gyldigMeldingOverføre.copy(
            type = Meldingstype.OVERFORING,
            overføring = Overføre(
                mottakerType = Mottaker.SAMBOER,
                antallDagerSomSkalOverføres = 0
            )
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile hvis type er OVERFØRE men mottakerType er samværsforelder `() {
        val melding = MeldingUtils.gyldigMeldingOverføre.copy(
            type = Meldingstype.OVERFORING,
            overføring = Overføre(
                mottakerType = Mottaker.SAMVÆRSFORELDER,
                antallDagerSomSkalOverføres = 1
            )
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    //--- Fordeling
    @Test
    fun `Skal ikke feile på gyldig fordeling`() {
        val melding = MeldingUtils.gyldigMeldingFordele
        melding.valider()
    }

    @Test
    fun `Skal feile hvis type er FORDELE men fordeling er null`() {
        val melding = MeldingUtils.gyldigMeldingFordele.copy(
            type = Meldingstype.FORDELING,
            fordeling = null
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

    @Test
    fun `Skal feile hvis type er FORDELE men mottakerType ikke er samværsforelder`() {
        val melding = MeldingUtils.gyldigMeldingFordele.copy(
            type = Meldingstype.FORDELING,
            fordeling = Fordele(
                mottakerType = Mottaker.SAMBOER,
                samværsavtale = listOf(URL("http://localhost:8080/vedlegg/1"))
            )
        )
        assertThrows<Throwblem> {
            melding.valider()
        }
    }

}
