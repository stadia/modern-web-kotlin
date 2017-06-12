package verticles

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import model.DataSourceConfig
import model.ServerConfig
import model.SunWeatherInfo
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import services.DatabaseAuthProvider
import services.MigrationService
import services.SunService
import services.WeatherService
import uy.klutter.vertx.VertxInit

/**
 * Created by jeff.yd on 09/06/2017.
 */
class MainVerticle : AbstractVerticle() {
    private var dataSource: HikariDataSource? = null

    private fun initDataSource(config: DataSourceConfig): HikariDataSource {
        val hikariDS = HikariDataSource()
        hikariDS.username = config.user
        hikariDS.password = config.password
        hikariDS.jdbcUrl = config.jdbcUrl
        dataSource = hikariDS
        return hikariDS
    }

    private var templateEngine: ThymeleafTemplateEngine = ThymeleafTemplateEngine.create()

    private var logger = LoggerFactory.getLogger("VertxServer")

    override fun start(startFuture: Future<Void>?) {
        VertxInit.ensure()
        val jsonMapper = jacksonObjectMapper()
        val serverConfig = jsonMapper.readValue(config().getJsonObject("server").encode(), ServerConfig::class.java)
        val serverPort = serverConfig.port
        val enableCaching = serverConfig.caching
        val server = vertx.createHttpServer()

        val dataSourceConfig = jsonMapper.readValue(config().getJsonObject("dataSource").encode(), DataSourceConfig::class.java)
        val dataSource = initDataSource(dataSourceConfig)
        val migrationService = MigrationService(dataSource)
        val migrationResult = migrationService.migrate()
        migrationResult.fold({ exc ->
            logger.fatal("Exception occurred while performing migration", exc)
            vertx.close()
        }, { res ->
            logger.info("Migration successful or not needed")
        })

        val router = Router.router(vertx)
        val weatherService  = WeatherService()
        val sunService = SunService()
        val staticHandler = StaticHandler.create().setWebRoot("public").setCachingEnabled(enableCaching)

        val authProvider = DatabaseAuthProvider(dataSource, jsonMapper)
        router.route().handler(CookieHandler.create())
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
        router.route().handler(UserSessionHandler.create(authProvider))
        router.route("/hidden/*").handler(RedirectAuthHandler.create(authProvider))
        router.route("/login").handler(BodyHandler.create())
        router.route("/login").handler(FormLoginHandler.create(authProvider))
        router.route("/public/*").handler(staticHandler)

        router.get("/").handler { routingContext ->
            val response = routingContext.response()
            response.end("Hello World!!")
        }

        router.get("/api/data").handler { context ->
            val lat = 37.401873563159754
            val lon = 127.10872923862162
            val sunInfoP = sunService.getSunInfo(lat, lon)
            val temperatureP = weatherService.getTemperature(lat, lon)

            val sunWeatherInfoP = sunInfoP.bind { sunInfo ->
                temperatureP.map { temp -> SunWeatherInfo(sunInfo, temp!!) }
            }

            sunWeatherInfoP.success { info ->
                val json = jsonMapper.writeValueAsString(info)
                context.response().end(json)
            }.fail { e ->
                logger.error(e)
            }
        }

        router.get("/loginpage").handler { ctx ->
            renderTemplate(ctx,"public/templates/login.html" )
        }

        router.get("/home").handler { context ->
            renderTemplate(context, "public/templates/index.html")
        }

        router.get("/hidden/admin").handler { ctx ->
            renderTemplate(ctx.put("username", ctx.user().principal().getString("username")),
                    "public/templates/admin.html")
        }

        server.requestHandler { router.accept(it) }
                .listen(serverPort, { handler ->
                    if (!handler.succeeded()) {
                        logger.error("Failed to listen on port 8080")
                    }
                })
    }

    fun renderTemplate(context: RoutingContext, template: String) {
        templateEngine.render(context, template, { buf ->
            val response = context.response()
            if (buf.failed()) {
                logger.error("Template rendering failed", buf.cause())
                response.setStatusCode(500).end()
            } else {
                response.end(buf.result())
            }
        })
    }

    override fun stop(stopFuture: Future<Void>?) {
        dataSource?.close()
    }
}