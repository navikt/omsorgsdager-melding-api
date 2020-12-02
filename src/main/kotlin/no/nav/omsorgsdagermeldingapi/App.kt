package no.nav.omsorgsdagermeldingapi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.metrics.micrometer.*
import io.ktor.routing.*
import io.ktor.util.*
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.dusseldorf.ktor.auth.allIssuers
import no.nav.helse.dusseldorf.ktor.auth.multipleJwtIssuers
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.health.HealthReporter
import no.nav.helse.dusseldorf.ktor.health.HealthRoute
import no.nav.helse.dusseldorf.ktor.health.HealthService
import no.nav.helse.dusseldorf.ktor.jackson.JacksonStatusPages
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.omsorgsdagermeldingapi.barn.BarnGateway
import no.nav.omsorgsdagermeldingapi.barn.BarnService
import no.nav.omsorgsdagermeldingapi.barn.barnApis
import no.nav.omsorgsdagermeldingapi.general.auth.IdTokenProvider
import no.nav.omsorgsdagermeldingapi.general.auth.IdTokenStatusPages
import no.nav.omsorgsdagermeldingapi.kafka.SøknadKafkaProducer
import no.nav.omsorgsdagermeldingapi.mellomlagring.MellomlagringService
import no.nav.omsorgsdagermeldingapi.mellomlagring.mellomlagringApis
import no.nav.omsorgsdagermeldingapi.redis.RedisConfig
import no.nav.omsorgsdagermeldingapi.redis.RedisConfigurationProperties
import no.nav.omsorgsdagermeldingapi.redis.RedisStore
import no.nav.omsorgsdagermeldingapi.søker.SøkerGateway
import no.nav.omsorgsdagermeldingapi.søker.SøkerService
import no.nav.omsorgsdagermeldingapi.søker.søkerApis
import no.nav.omsorgsdagermeldingapi.søknad.SøknadService
import no.nav.omsorgsdagermeldingapi.søknad.søknadApis
import no.nav.omsorgsdagermeldingapi.vedlegg.K9MellomlagringGateway
import no.nav.omsorgsdagermeldingapi.vedlegg.VedleggService
import no.nav.omsorgsdagermeldingapi.vedlegg.vedleggApis
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private val logger: Logger = LoggerFactory.getLogger("nav.omsorgpengermidlertidigaleneapi")

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Application.omsorgpengermidlertidigaleneapi() {
    
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    System.setProperty("dusseldorf.ktor.serializeProblemDetailsWithContentNegotiation", "true")

    val configuration = Configuration(environment.config)
    val apiGatewayApiKey = configuration.getApiGatewayApiKey()

    install(ContentNegotiation) {
        jackson {
            dusseldorfConfigured()
                .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
                .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Delete)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        log.info("Configuring CORS")
        configuration.getWhitelistedCorsAddreses().forEach {
            log.info("Adding host {} with scheme {}", it.host, it.scheme)
            host(host = it.authority, schemes = listOf(it.scheme))
        }
    }

    val idTokenProvider = IdTokenProvider(cookieName = configuration.getCookieName())
    val issuers = configuration.issuers()

    install(Authentication) {
        multipleJwtIssuers(
            issuers = issuers,
            extractHttpAuthHeader = {call ->
                idTokenProvider.getIdToken(call)
                    .somHttpAuthHeader()
            }
        )
    }

    install(StatusPages) {
        DefaultStatusPages()
        JacksonStatusPages()
        IdTokenStatusPages()
    }

    install(Locations)

    install(Routing) {

        val søkerGateway = SøkerGateway(
            baseUrl = configuration.getK9OppslagUrl(),
            apiGatewayApiKey = apiGatewayApiKey
        )

        val søkerService = SøkerService(
            søkerGateway = søkerGateway
        )

        val barnGateway = BarnGateway(
            baseUrl = configuration.getK9OppslagUrl(),
            apiGatewayApiKey = apiGatewayApiKey
        )

        val barnService = BarnService(
            barnGateway = barnGateway,
            cache = configuration.cache()
        )

        val vedleggService = VedleggService(
            k9MellomlagringGateway = K9MellomlagringGateway(
                baseUrl = configuration.getK9MellomlagringUrl()
            )
        )

        val søknadKafkaProducer = SøknadKafkaProducer(
            kafkaConfig = configuration.getKafkaConfig()
        )

        environment.monitor.subscribe(ApplicationStopping) {
            logger.info("Stopper Kafka Producer.")
            søknadKafkaProducer.stop()
            logger.info("Kafka Producer Stoppet.")
        }

        authenticate(*issuers.allIssuers())  {

            søkerApis(
                søkerService = søkerService,
                idTokenProvider = idTokenProvider
            )

            barnApis(
                barnService = barnService,
                idTokenProvider = idTokenProvider
            )

            mellomlagringApis(
                mellomlagringService = MellomlagringService(
                    RedisStore(
                        RedisConfig(
                            RedisConfigurationProperties(
                                configuration.getRedisHost().equals("localhost")
                            )
                        ).redisClient(configuration)
                    ), configuration.getStoragePassphrase()),
                idTokenProvider = idTokenProvider
            )

            vedleggApis(
                vedleggService = vedleggService,
                idTokenProvider = idTokenProvider
            )

            søknadApis(
                idTokenProvider = idTokenProvider,
                søknadService = SøknadService(
                    søkerService = søkerService,
                    kafkaProducer = søknadKafkaProducer
                )
            )
        }

        val healthService = HealthService(
            healthChecks = setOf(
                søknadKafkaProducer,
                søkerGateway
            )
        )

        HealthReporter(
            app = appId,
            healthService = healthService,
            frequency = Duration.ofMinutes(1)
        )

        DefaultProbeRoutes()
        MetricsRoute()
        HealthRoute(
            healthService = healthService
        )
    }

    install(MicrometerMetrics) {
        init(appId)
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        call.request.log()
    }

    install(CallId) {
        generated()
    }

    install(CallLogging) {
        correlationIdAndRequestIdInMdc()
        logRequests()
        mdc("id_token_jti") { call ->
            try { idTokenProvider.getIdToken(call).getId() }
            catch (cause: Throwable) { null }
        }
    }
}

fun ObjectMapper.k9MellomlagringKonfigurert(): ObjectMapper {
    return jacksonObjectMapper().dusseldorfConfigured().apply {
        configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    }
}

fun ObjectMapper.k9SelvbetjeningOppslagKonfigurert(): ObjectMapper {
    return jacksonObjectMapper().dusseldorfConfigured().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        registerModule(JavaTimeModule())
        propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    }
}