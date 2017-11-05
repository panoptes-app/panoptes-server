package org.panoptes.server.core.http

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import org.panoptes.server.core.Constants

class MockUserCreatorVerticle : AbstractVerticle() {

    @Throws(Exception::class)
    override fun start() {
        vertx.eventBus().consumer<JsonObject>(Constants.TOPIC_CREATE_USER, { msg ->
            if (msg.body().getString("login") == "toto" && msg.body().getString("password") == "tutu") {
                msg.reply("toto")
            } else {
                msg.fail(HttpResponseStatus.CONFLICT.code(),"user already exists")
            }
         })
    }

}
