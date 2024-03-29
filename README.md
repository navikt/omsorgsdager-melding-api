OBS: Erstattet av [K9-brukerdialog-api](https://github.com/navikt/k9-brukerdialog-api)

# Omsorgsdager-melding-api

![CI / CD](https://github.com/navikt/omsorgsdager-melding-api/workflows/CI%20/%20CD/badge.svg)
![NAIS Alerts](https://github.com/navikt/omsorgsdager-melding-api/workflows/Alerts/badge.svg)

# Innholdsoversikt
* [1. Kontekst](#1-kontekst)
* [2. Funksjonelle Krav](#2-funksjonelle-krav)
* [3. Begrensninger](#3-begrensninger)
* [4. Distribusjon av tjenesten (deployment)](#9-distribusjon-av-tjenesten-deployment)
* [5. Utviklingsmiljø](#10-utviklingsmilj)
* [6. Drift og støtte](#11-drift-og-sttte)

# 1. Kontekst
API for koronaoverføring, overføring og fordeling av dager

# 2. Funksjonelle Krav
Denne tjenesten understøtter søknadsprosessen, samt eksponerer endepunkt for innsending av melding.

API mottar meldingene, validerer og legger dem videre på en kafka-topic som 
omsorgsdager-melding-prosessering konsumerer.

Vedlegg blir persistert når meldingen blir sendt inn til api. Dersom noe skulle gå galt i api
som gjør at meldingen ikke blir lagt på Kafka, så slettes vedlegget. 

# 3. Endepunkter
**GET @/soker --> Gir 200 respons med json av søker**
```
{ 
    "aktør_id": "23456",
    "fornavn": "ARNE",
    "mellomnavn": "BJARNE",
    "etternavn": "CARLSEN",
    "fødselsdato": "1990-01-02"
}
```

**POST /vedlegg --> For å lagre vedlegg**

**DELETE /vedlegg/{vedleggsid} --> For å slette vedlegg**

**POST @/melding/koronaoverforing --> 202 repons ved gyldig melding om koronaoverføring av omsorgsdager**

**POST @/melding/overforing --> 202 repons ved gyldig melding om overføring av omsorgsdager**

**POST @/melding/fordeling --> 202 repons ved gyldig melding om fordeling av omsorgsdager**

Ved valideringsfeil får man tilbake 400 og liste over valideringsbrudd.

**Validering**

Se [MeldingValidator.kt](https://github.com/navikt/omsorgsdager-melding-api/blob/master/src/main/kotlin/no/nav/omsorgsdagermeldingapi/s%C3%B8knad/melding/MeldingValidator.kt)

Eksempel json;
```
{
  "id": "123456789",
  "språk": "nb",
  "harForståttRettigheterOgPlikter": true,
  "harBekreftetOpplysninger": true
}
```

# 4. Distribusjon av tjenesten (deployment)
Distribusjon av tjenesten er gjort med bruk av Github Actions.
[Omsorgsdager-melding-api CI / CD](https://github.com/navikt/omsorgsdager-melding-api/actions)

Push til dev-* brancher vil teste, bygge og deploye til dev/staging miljø.
Push/merge til master branche vil teste, bygge og deploye til produksjonsmiljø.

# 5. Utviklingsmiljø
## Bygge Prosjekt
For å bygge kode, kjør:

```shell script
./gradlew clean build
```

## Kjøre Prosjekt
For å kjøre kode, kjør:

```shell script
./gradlew bootRun
```

# 6. Drift og støtte
## Logging
[Kibana](https://tinyurl.com/ydkqetfo)

## Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [nais/alerterator.yml](nais/alerterator.yml).

# Metrics
n/a

### Redis
Vi bruker Redis for mellomlagring. En instanse av Redis må være kjørene før deploy av applikasjonen. 
Dette gjøres manuelt med kubectl både i preprod og prod. Se [nais/doc](https://github.com/nais/doc/blob/master/content/redis.md)
