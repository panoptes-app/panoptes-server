package org.panoptes.server.core.http

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.client.WebClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.panoptes.server.core.MainVerticle
import java.net.ServerSocket


@RunWith(VertxUnitRunner::class)
class HttpAuthTest {

    private lateinit var vertx: Vertx
    private var port: Int = 0


    @Before
    fun before(context: TestContext) {
        val socket = ServerSocket(0)
        port = socket.localPort
        socket.close()

        val options = DeploymentOptions()
            .setConfig(JsonObject().put("http.port", port))
        vertx = Vertx.vertx()
        vertx.deployVerticle(MockUserCreatorVerticle::class.java.name, context.asyncAssertSuccess())
        vertx.deployVerticle(MockAuthenticatorVerticle::class.java.name, context.asyncAssertSuccess())
        vertx.deployVerticle(MainVerticle::class.java.name, options, context.asyncAssertSuccess())
    }



    @After
    fun after(context: TestContext) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    fun testRefuseAuth(context: TestContext) {
        // Send a request and get a response
        val client = vertx.createHttpClient()
        val async = context.async()
        client.getNow(port.toInt(), "localhost", "/auth") { resp ->
            context.assertEquals( HttpResponseStatus.FORBIDDEN.code(), resp.statusCode())
            async.complete()
        }
    }

    @Test
    fun testCreateUser(context: TestContext) {
        // Send a request and get a response
        val client = WebClient.create(vertx)
        val async = context.async()
        client.post(port.toInt(), "localhost", "/user")
            .addQueryParam("login", "toto")
            .addQueryParam("password","tutu")
            .send { resp ->
                context.assertEquals(HttpResponseStatus.CREATED.code(), resp.result().statusCode())
                async.complete()
            }
    }

    @Test
    fun testAcceptAuth(context: TestContext) {
        // Send a request and get a response
        val client = WebClient.create(vertx)
        val async = context.async()
        client.get(port.toInt(), "localhost", "/auth")
            .addQueryParam("login", "toto")
            .addQueryParam("password","tutu")
            .send { resp ->
                context.assertEquals(HttpResponseStatus.OK.code(), resp.result().statusCode())
                async.complete()
            }
    }



}
