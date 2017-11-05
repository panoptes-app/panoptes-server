package org.panoptes.server.core

import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.mqtt.MqttConnectReturnCode
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.mqtt.MqttServerOptions
import io.vertx.mqtt.MqttClient
import io.vertx.mqtt.MqttClientOptions
import io.vertx.mqtt.MqttServer

class MainVerticle : AbstractVerticle() {

    @Throws(Exception::class)
    override fun start() {
        val httpPort = config().getInteger("http.port", 8080)!!
        val mqttPort = config().getInteger("mqtt.port", 1883)!!

        startHttpServer(httpPort)
        startMqttBroker(mqttPort)

    }

    private fun startMqttBroker(mqttPort: Int) {
        val mqttServerOptions = MqttServerOptions(clientAuthRequired = true, port = mqttPort, autoClientId = true)
        val mqttServer = MqttServer.create(vertx, mqttServerOptions)
        mqttServer.endpointHandler { endpoint ->

            // shows main connect info
            println("MQTT client [" + endpoint.clientIdentifier() + "] request to connect, clean session = " + endpoint.isCleanSession)

            if (endpoint.auth() != null) {
                println("try to authenticate [username = " + endpoint.auth().userName() + ", password = " + endpoint.auth().password() + "]")
                vertx.eventBus().send<Boolean>(Constants.TOPIC_AUTH_USER, JsonObject().put("login", endpoint.auth().userName()).put("password", endpoint.auth().password()), { response ->
                    if (!response.succeeded() || !response.result().body()) {
                        endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED)
                    } else {
                        println("authenticate [username = " + endpoint.auth().userName() + ", password = " + endpoint.auth().password() + "] => OK")
                        if (endpoint.will() != null) {
                            println("[will topic = " + endpoint.will().willTopic() + " msg = " + endpoint.will().willMessage() +
                                    " QoS = " + endpoint.will().willQos() + " isRetain = " + endpoint.will().isWillRetain + "]")
                        }

                        println("[keep alive timeout = " + endpoint.keepAliveTimeSeconds() + "]")

                        // accept connection from the remote client
                        endpoint.accept(false)
                    }
                })

            } else {
                endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED)
            }


        }.listen { ar ->
            if (ar.succeeded()) {
                println("MQTT server is listening on port " + ar.result().actualPort())
            } else {
                println("Error on starting the server")
                ar.cause().printStackTrace()
            }
        }.endpointHandler({ handler ->
            handler.publishHandler({ message ->
                println("msg published on topic ${message.topicName()} : ${String(message.payload().getBytes())}")
            })
        })
    }

    private fun startHttpServer(httpPort: Int) {
        val router = Router.router(vertx)
        router.route("/auth").handler { routingContext ->
            if (routingContext.request().method() == HttpMethod.GET) {
                val login = routingContext.request().getParam("login")
                val password = routingContext.request().getParam("password")
                checkLogin(login, password, routingContext)
            } else {
                routingContext.fail(HttpResponseStatus.BAD_REQUEST.code())
            }
        }
        router.route("/user").handler { routingContext ->

            if (routingContext.request().method() == HttpMethod.POST) {
                val login = routingContext.request().getParam("login")
                val password = routingContext.request().getParam("password")
                createUser(login, password, routingContext)
            } else {
                routingContext.fail(HttpResponseStatus.BAD_REQUEST.code())
            }
        }
        vertx.createHttpServer().requestHandler(router::accept).listen(httpPort)
        println("HTTP server started on port ${httpPort}")
    }

    private fun createUser(login: String?, password: String?, routingContext: RoutingContext) {
        if (login != null && password != null) {
            vertx.eventBus().send<String>(Constants.TOPIC_CREATE_USER, JsonObject().put("login",login).put("password", password),{ response ->
                if (response.succeeded()) {
                    routingContext.response().statusCode = HttpResponseStatus.CREATED.code()
                    routingContext.response().end(response.result().body())

                } else {
                    routingContext.fail(response.cause())
                }

            } )
        } else {
            routingContext.fail(HttpResponseStatus.FORBIDDEN.code())
        }

    }

    private fun checkLogin(login: String?, password: String?, routingContext: RoutingContext) {
        if (login != null && password != null) {
            vertx.eventBus().send<Boolean>(Constants.TOPIC_AUTH_USER, JsonObject().put("login",login).put("password", password),{ response ->
                if (response.succeeded() && response.result().body()) {
                    routingContext.response().statusCode = HttpResponseStatus.OK.code()
                } else {
                    routingContext.fail( HttpResponseStatus.FORBIDDEN.code())
                }

            } )
        } else {
            routingContext.response().statusCode = HttpResponseStatus.FORBIDDEN.code()
        }
        routingContext.response().end()
    }
}
