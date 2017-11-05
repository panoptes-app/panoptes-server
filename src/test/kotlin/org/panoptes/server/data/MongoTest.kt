package org.panoptes.server.data

import io.vertx.core.AsyncResult
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.panoptes.server.core.Constants
import org.panoptes.server.core.MongoVerticle

@RunWith(VertxUnitRunner::class)
class MongoTest {

    private lateinit var vertx: Vertx


    @Before
    fun before(context: TestContext) {
        vertx = Vertx.vertx()
        val options = DeploymentOptions().setConfig(JsonObject().put("mongo_db_name", "panoptes").put("mongo_connection_string", "mongodb://localhost:27017"))
        vertx.deployVerticle(MongoVerticle::class.java.name, options, context.asyncAssertSuccess())
    }

    @After
    fun after(context: TestContext) {
        vertx.close(context.asyncAssertSuccess())
    }

    @Test
    fun testInsertUser(context: TestContext) {
        val async = context.async()
        val user = JsonObject().put("login", "toto").put("password", "tutu")
        vertx.eventBus().send(Constants.TOPIC_CREATE_USER,user, { userCreation: AsyncResult<Message<Boolean>> ->
            if (userCreation.succeeded()){
                Assert.assertTrue( userCreation.result().body())
            }
            async.complete()
        })
    }

    @Test
    fun testAuthUser(context: TestContext) {
        val async = context.async()
        val user = JsonObject().put("login", "toto").put("password", "tutu")
        vertx.eventBus().send(Constants.TOPIC_AUTH_USER,user, { userCreation: AsyncResult<Message<Boolean>> ->
            if (userCreation.succeeded()){
                Assert.assertTrue( userCreation.result().body())
            }
            async.complete()
        })
    }

    @Test
    fun testNonAuthUser(context: TestContext) {
        val async = context.async()
        val user = JsonObject().put("login", "toto").put("password", "bad_password")
        vertx.eventBus().send(Constants.TOPIC_AUTH_USER,user, { userCreation: AsyncResult<Message<Boolean>> ->
            if (userCreation.succeeded()){
                Assert.assertFalse( userCreation.result().body())
            }
            async.complete()
        })
    }
}
