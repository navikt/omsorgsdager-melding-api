package no.nav.omsorgsdagermeldingapi.søknad.melding

import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.omsorgsdagermeldingapi.vedlegg.Vedlegg
import java.net.URL
import java.time.format.DateTimeFormatter

private const val MAX_VEDLEGG_SIZE = 24 * 1024 * 1024 // 24 MB
private val vedleggTooLargeProblemDetails = DefaultProblemDetails(
    title = "attachments-too-large",
    status = 413,
    detail = "Totale størreslsen på alle vedlegg overstiger maks på 24 MB."
)

private val KUN_SIFFER = Regex("\\d+")
internal val vekttallProviderFnr1: (Int) -> Int = { arrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2).reversedArray()[it] }
internal val vekttallProviderFnr2: (Int) -> Int = { arrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2).reversedArray()[it] }
private val fnrDateFormat = DateTimeFormatter.ofPattern("ddMMyy")

internal val MAX_ANTALL_DAGER_MAN_KAN_OVERFØRE = 10
internal val MAX_ANTALL_DAGER_MAN_KAN_KORONA_OVERFØRE = 999
internal val MIN_ANTALL_DAGER_MAN_KAN_OVERFØRE = 1

internal fun Melding.valider() {
    val mangler: MutableSet<Violation> = mutableSetOf()

    if (harBekreftetOpplysninger er false) {
        mangler.add(
            Violation(
                parameterName = "harBekreftetOpplysninger",
                parameterType = ParameterType.ENTITY,
                reason = "Opplysningene må bekreftes for å sende inn melding.",
                invalidValue = harBekreftetOpplysninger
            )
        )
    }

    if (harForståttRettigheterOgPlikter er false) {
        mangler.add(
            Violation(
                parameterName = "harForståttRettigheterOgPlikter",
                parameterType = ParameterType.ENTITY,
                reason = "Må ha forstått rettigheter og plikter for å sende inn melding.",
                invalidValue = harForståttRettigheterOgPlikter
            )
        )
    }

    if(mottakerNavn.isNullOrBlank()){
        mangler.add(
            Violation(
                parameterName = "mottakerNavn",
                parameterType = ParameterType.ENTITY,
                reason = "mottakerNavn kan ikke være null, tom eller bare mellomrom",
                invalidValue = mottakerNavn
            )
        )
    }

    if(mottakerFnr.erGyldigNorskIdentifikator() er false){
        mangler.add(
            Violation(
                parameterName = "mottakerFnr",
                parameterType = ParameterType.ENTITY,
                reason = "mottakerFnr må være gyldig norsk identifikator",
                invalidValue = mottakerFnr
            )
        )
    }

    if(arbeidssituasjon.isEmpty()){
        mangler.add(
            Violation(
                parameterName = "arbeidssituasjon",
                parameterType = ParameterType.ENTITY,
                reason = "arbeidssituasjon kan ikke være en tom liste",
                invalidValue = arbeidssituasjon
            )
        )
    }

    if(barn.isEmpty()){
        mangler.add(
            Violation(
                parameterName = "barn",
                parameterType = ParameterType.ENTITY,
                reason = "barn kan ikke være en tom liste",
                invalidValue = barn
            )
        )
    }

    barn.forEachIndexed { index, barnUtvidet ->  mangler.addAll(barnUtvidet.valider(index))}

    when(type){
        Meldingstype.KORONA -> mangler.addAll(validerKoronaOverføre())
        Meldingstype.OVERFORING -> mangler.addAll(validerOverføre())
        Meldingstype.FORDELING -> mangler.addAll(validerFordele())
    }

    mangler.addAll(nullSjekk(harAleneomsorg, "harAleneomsorg"))
    mangler.addAll(nullSjekk(harUtvidetRett, "harUtvidetRett"))
    mangler.addAll(nullSjekk(erYrkesaktiv, "erYrkesaktiv"))
    mangler.addAll(nullSjekk(arbeiderINorge, "arbeiderINorge"))

    if (mangler.isNotEmpty()) {
        throw Throwblem(ValidationProblemDetails(mangler))
    }
}

fun String.erKunSiffer() = matches(KUN_SIFFER)

fun String.starterMedFodselsdato(): Boolean {
    // Sjekker ikke hvilket århundre vi skal tolket yy som, kun at det er en gyldig dato.
    // F.eks blir 290990 parset til 2090-09-29, selv om 1990-09-29 var ønskelig.
    // Kunne sett på individsifre (Tre første av personnummer) for å tolke århundre,
    // men virker unødvendig komplekst og sårbart for ev. endringer i fødselsnummeret.
    return try {
        var substring = substring(0, 6)
        val førsteSiffer = (substring[0]).toString().toInt()
        if (førsteSiffer in 4..7) {
            substring = (førsteSiffer - 4).toString() + substring(1, 6)
        }
        fnrDateFormat.parse(substring)

        true
    } catch (cause: Throwable) {
        false
    }
}

fun String.erGyldigNorskIdentifikator(): Boolean {
    if (length != 11 || !erKunSiffer() || !starterMedFodselsdato()) return false

    val forventetKontrollsifferEn = get(9)

    val kalkulertKontrollsifferEn = Mod11.kontrollsiffer(
        number = substring(0, 9),
        vekttallProvider = vekttallProviderFnr1
    )

    if (kalkulertKontrollsifferEn != forventetKontrollsifferEn) return false

    val forventetKontrollsifferTo = get(10)

    val kalkulertKontrollsifferTo = Mod11.kontrollsiffer(
        number = substring(0, 10),
        vekttallProvider = vekttallProviderFnr2
    )

    return kalkulertKontrollsifferTo == forventetKontrollsifferTo
}

/**
 * https://github.com/navikt/helse-sparkel/blob/2e79217ae00632efdd0d4e68655ada3d7938c4b6/src/main/kotlin/no/nav/helse/ws/organisasjon/Mod11.kt
 * https://www.miles.no/blogg/tema/teknisk/validering-av-norske-data
 */
internal object Mod11 {
    private val defaultVekttallProvider: (Int) -> Int = { 2 + it % 6 }

    internal fun kontrollsiffer(
        number: String,
        vekttallProvider: (Int) -> Int = defaultVekttallProvider
    ): Char {
        return number.reversed().mapIndexed { i, char ->
            Character.getNumericValue(char) * vekttallProvider(i)
        }.sum().let(Mod11::kontrollsifferFraSum)
    }


    private fun kontrollsifferFraSum(sum: Int) = sum.rem(11).let { rest ->
        when (rest) {
            0 -> '0'
            1 -> '-'
            else -> "${11 - rest}"[0]
        }
    }
}

internal fun nullSjekk(verdi: Boolean?, navn: String): MutableSet<Violation>{
    val mangler: MutableSet<Violation> = mutableSetOf<Violation>()

    if(verdi er null){
        mangler.add(
            Violation(
                parameterName = navn,
                parameterType = ParameterType.ENTITY,
                reason = "$navn kan ikke være null",
                invalidValue = verdi
            )
        )
    }

    return mangler
}

internal infix fun Boolean?.er(forventetVerdi: Boolean?): Boolean = this == forventetVerdi

internal fun List<Vedlegg>.validerVedlegg(vedleggUrler: List<URL>) {
    if (size != vedleggUrler.size) {
        throw Throwblem(
            ValidationProblemDetails(
                violations = setOf(
                    Violation(
                        parameterName = "vedlegg",
                        parameterType = ParameterType.ENTITY,
                        reason = "Mottok referanse til ${vedleggUrler.size} vedlegg, men fant kun $size vedlegg.",
                        invalidValue = vedleggUrler
                    )
                )
            )
        )
    }
    validerTotalStorresle()
}

private fun List<Vedlegg>.validerTotalStorresle() {
    val totalSize = sumBy { it.content.size }
    if (totalSize > MAX_VEDLEGG_SIZE) {
        throw Throwblem(vedleggTooLargeProblemDetails)
    }
}
