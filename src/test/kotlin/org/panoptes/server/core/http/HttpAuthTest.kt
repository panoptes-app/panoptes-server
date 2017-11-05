package org.panoptes.server.core.http

import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.mqtt.MqttQoS
import io.vertx.core.AsyncResult
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import io.vertx.mqtt.MqttClient
import io.vertx.mqtt.MqttClientOptions
import io.vertx.mqtt.messages.MqttConnAckMessage
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.panoptes.server.core.MainVerticle
import java.net.ServerSocket


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(VertxUnitRunner::class)
class HttpAuthTest {

    private lateinit var vertx: Vertx
    private var httpPort: Int = 0
    private var mqttPort: Int = 0

    @Before
    fun before(context: TestContext) {
        httpPort = getNewPort()
        mqttPort = getNewPort()

        val options = DeploymentOptions().setConfig(JsonObject().put("http.port", httpPort).put("mqtt.port", mqttPort))
        vertx = Vertx.vertx()
        vertx.deployVerticle(MockUserCreatorVerticle::class.java.name, context.asyncAssertSuccess())
        vertx.deployVerticle(MockAuthenticatorVerticle::class.java.name, context.asyncAssertSuccess())
        vertx.deployVerticle(MainVerticle::class.java.name, options, context.asyncAssertSuccess())
    }

    private fun getNewPort(): Int{
        val httpsocket = ServerSocket(0)
        val port = httpsocket.localPort
        httpsocket.close()
        return port
    }


    @After
    fun after(context: TestContext) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    fun test1RefuseAuth(context: TestContext) {
        // Send a request and get a response
        val client = vertx.createHttpClient()
        val async = context.async()
        client.getNow(httpPort, "localhost", "/auth") { resp ->
            context.assertEquals( HttpResponseStatus.FORBIDDEN.code(), resp.statusCode())
            async.complete()
        }
    }

    @Test
    fun test2CreateUser(context: TestContext) {
        // Send a request and get a response
        val client = WebClient.create(vertx)
        val async = context.async()
        client.post(httpPort, "localhost", "/user")
            .addQueryParam("login", "toto")
            .addQueryParam("password","tutu")
            .send { resp ->
                if (resp.succeeded()) {
                    context.assertEquals(HttpResponseStatus.CREATED.code(), resp.result().statusCode())
                    async.complete()
                }else {
                    async.resolve(Future.failedFuture(resp.cause()))
                }
            }
    }



    @Test
    fun test3AcceptAuth(context: TestContext) {
        // Send a request and get a response
        val client = WebClient.create(vertx)
        val async = context.async()
        client.get(httpPort, "localhost", "/auth")
            .addQueryParam("login", "toto")
            .addQueryParam("password","tutu")
            .send { resp ->
                context.assertEquals(HttpResponseStatus.OK.code(), resp.result().statusCode())
                async.complete()
            }
    }

    @Test
    fun test4MqttWithoutAuthFail(context: TestContext) {
        val mqttClient = MqttClient.create(vertx)
        mqttClient.connect(mqttPort, "localhost", context.asyncAssertFailure())
    }

    @Test
    fun test5FailMqttAuthWithWrongLogin(context: TestContext) {
        val mqttClientOptions = MqttClientOptions()
        mqttClientOptions.setUsername("mackristof")
        mqttClientOptions.setPassword("tutu")
        val mqttClient = MqttClient.create(vertx, mqttClientOptions)
        mqttClient.connect(mqttPort, "localhost", context.asyncAssertFailure())
    }

    @Test
    fun test6FailMqttAuthWithWrongPassword(context: TestContext) {
        val mqttClientOptions = MqttClientOptions()
        mqttClientOptions.setUsername("toto")
        mqttClientOptions.setPassword("toto")
        val mqttClient = MqttClient.create(vertx, mqttClientOptions)
        mqttClient.connect(mqttPort, "localhost", context.asyncAssertFailure())
    }

    @Test
    fun test7AcceptMqttAuth(context: TestContext) {
        val mqttClientOptions = MqttClientOptions()
        mqttClientOptions.setUsername("toto")
        mqttClientOptions.setPassword("tutu")
        val mqttClient = MqttClient.create(vertx, mqttClientOptions)
        mqttClient.connect(mqttPort, "localhost", context.asyncAssertSuccess())
    }


    @Test
    fun testAcceptMqttPublish(context: TestContext) {
        val mqttClientOptions = MqttClientOptions()
        mqttClientOptions.setUsername("toto")
        mqttClientOptions.setPassword("tutu")
        val async = context.async()
        var mqttClient = MqttClient.create(vertx, mqttClientOptions)
        mqttClient.connect(mqttPort, "localhost", {result: AsyncResult<MqttConnAckMessage>? ->
            if (result != null  && result.succeeded()) {
                mqttClient.publish("/test", Buffer.buffer("toto"), MqttQoS.AT_LEAST_ONCE, false, true, { _ : AsyncResult<Int>? ->
                    async.complete()
                })
            } else {
                throw Exception(result?.cause())
            }
        })


    }


}
