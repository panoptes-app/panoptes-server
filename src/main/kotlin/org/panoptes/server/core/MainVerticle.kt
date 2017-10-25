package org.panoptes.server.core

import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.mqtt.MqttConnectReturnCode
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.mqtt.MqttServer

class MainVerticle : AbstractVerticle() {

    @Throws(Exception::class)
    override fun start() {
        val port = config().getInteger("http.port", 8080)!!
        val router = Router.router(vertx)
        router.route("/auth").handler{ event ->
            val response = event.response()
            val request = event.request()
            if ( request.method() == HttpMethod.GET) {
                val login = event.request().getParam("login")
                val password = event.request().getParam("password")
                checkLogin(login, password, response)
            } else {
                response.statusCode = HttpResponseStatus.BAD_REQUEST.code()
            }
            response.end()

        }
        router.route("/user").handler{ event ->
            val response = event.response()
            val request = event.request()
            if (request.method() == HttpMethod.POST){
                val login = event.request().getParam("login")
                val password = event.request().getParam("password")
                createUser(login, password, response)
            } else {
                response.statusCode = HttpResponseStatus.BAD_REQUEST.code()
                response.end()
            }
        }
        vertx.createHttpServer().requestHandler(router::accept).listen(port)
        println("HTTP server started on port ${port}")

        val mqttServer = MqttServer.create(vertx)
        mqttServer.endpointHandler { endpoint ->

            // shows main connect info
            println("MQTT client [" + endpoint.clientIdentifier() + "] request to connect, clean session = " + endpoint.isCleanSession)

            if (endpoint.auth() != null) {
                println("[username = " + endpoint.auth().userName() + ", password = " + endpoint.auth().password() + "]")
            } else {
                endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED)
            }
            if (endpoint.will() != null) {
                println("[will topic = " + endpoint.will().willTopic() + " msg = " + endpoint.will().willMessage() +
                    " QoS = " + endpoint.will().willQos() + " isRetain = " + endpoint.will().isWillRetain + "]")
            }

            println("[keep alive timeout = " + endpoint.keepAliveTimeSeconds() + "]")

            // accept connection from the remote client
            endpoint.accept(false)

        }
            .listen { ar ->

                if (ar.succeeded()) {

                    println("MQTT server is listening on port " + ar.result().actualPort())
                } else {

                    println("Error on starting the server")
                    ar.cause().printStackTrace()
                }
            }

    }

    private fun createUser(login: String?, password: String?, httpResponse: HttpServerResponse) {
        if (login != null && password != null) {
            vertx.eventBus().send<String>(Constants.TOPIC_CREATE_USER, JsonObject().put("login",login).put("password", password),{ response ->
                if (response.succeeded() && response.result().body()!= null) {
                    httpResponse.statusCode = HttpResponseStatus.CREATED.code()
                    httpResponse.end(response.result().body())

                } else {
                    httpResponse.statusCode = HttpResponseStatus.BAD_REQUEST.code()
                    httpResponse.end()
                }

            } )
        } else {
            httpResponse.statusCode = HttpResponseStatus.FORBIDDEN.code()
            httpResponse.end()
        }

    }

    private fun checkLogin(login: String?, password: String?, httpResponse: HttpServerResponse) {
        if (login != null && password != null) {
            vertx.eventBus().send<Boolean>(Constants.TOPIC_AUTH_USER, JsonObject().put("login",login).put("password", password),{ response ->
                if (response.succeeded() && response.result().body()) {
                    httpResponse.statusCode = HttpResponseStatus.OK.code()
                } else {
                    httpResponse.statusCode = HttpResponseStatus.FORBIDDEN.code()
                }

            } )
        } else {
            httpResponse.statusCode = HttpResponseStatus.FORBIDDEN.code()
        }

    }
}
