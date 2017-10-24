package org.panoptes.server.core

import io.vertx.core.AbstractVerticle
import io.vertx.mqtt.MqttServer

class MainVerticle : AbstractVerticle() {

    @Throws(Exception::class)
    override fun start() {

        vertx.createHttpServer().requestHandler { req ->
            req.response()
                .putHeader("content-type", "text/plain")
                .end("Hello from Vert.x!")
        }.listen(8080)
        println("HTTP server started on port 8080")

        val mqttServer = MqttServer.create(vertx)
        mqttServer.endpointHandler { endpoint ->

            // shows main connect info
            println("MQTT client [" + endpoint.clientIdentifier() + "] request to connect, clean session = " + endpoint.isCleanSession)

            if (endpoint.auth() != null) {
                println("[username = " + endpoint.auth().userName() + ", password = " + endpoint.auth().password() + "]")
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
}
