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

    if(mottakerFnr.erGyldigFodselsnummer() er false){
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
    val totalSize = sumOf { it.content.size }
    if (totalSize > MAX_VEDLEGG_SIZE) {
        throw Throwblem(vedleggTooLargeProblemDetails)
    }
}
