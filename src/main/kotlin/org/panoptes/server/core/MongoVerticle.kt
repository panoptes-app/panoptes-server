package org.panoptes.server.core

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient

class MongoVerticle : AbstractVerticle() {

    lateinit var client: MongoClient

    @Throws(Exception::class)
    override fun start() {
        val config = JsonObject().put("db_name", config().getString("mongo_db_name")).put("connection_string", config().getString("mongo_connection_string"))
        client = MongoClient.createShared(vertx, config, "MongoPool")
        initDb()
    }

    private fun suscribeCommand() {
        vertx.eventBus().consumer<JsonObject>(Constants.TOPIC_AUTH_USER, { msg ->
            val userToFind = JsonObject().put("email",msg.body().getString("login"))
            client.find(Constants.MONGO_COLLECTION_USERS, userToFind, { userResearch ->
                if (userResearch.succeeded() && !userResearch.result().isEmpty()){
                    if (userResearch.result().first().getString("password") == encodePassword(msg.body().getString("password"))){
                        msg.reply(true)
                    } else {
                        msg.fail(HttpResponseStatus.FORBIDDEN.code(), "user not found")
                    }
                } else {
                    println("could not find ${msg.body().getString("login")}, cause: ${userResearch.cause()}")
                }
            })
        })


        vertx.eventBus().consumer<JsonObject>(Constants.TOPIC_CREATE_USER, { msg ->
            val userToSearch = JsonObject().put("email",msg.body().getString("login"))
            client.find(Constants.MONGO_COLLECTION_USERS, userToSearch, { userResearch ->
                if (userResearch.succeeded().and(!userResearch.result().first().isEmpty)){
                    msg.fail(HttpResponseStatus.CONFLICT.code(), "user already exists")
                } else {
                    val userToInsert = userToSearch.put("password", encodePassword(msg.body().getString("password")))
                    client.save(Constants.MONGO_COLLECTION_USERS, userToInsert, { userInsertion ->
                        if (userInsertion.succeeded()){
                            msg.reply(userInsertion.result())
                        }
                    })
                }
            })
        })
    }

    private fun encodePassword(password: String): String {
        return password
    }

    private fun initDb() {
        println("initialize db")
        client.getCollections { res ->
            if ( res.succeeded() && !(res.result().stream().filter({ t -> t.equals(Constants.MONGO_COLLECTION_USERS) }).findFirst().isPresent)){
                //create collection and populate with technical user
                createCollections()
            } else {
                suscribeCommand()
            }
        }
    }

    private fun createCollections() {
        println("create users collection")
        client.createCollection(Constants.MONGO_COLLECTION_USERS, { creation ->
            if (creation.succeeded()) {
                createPanoptesUser()
            } else {
                println("cannot create users collections so kill ${this.javaClass.name} is dieing")
                this.stop()
            }
        })
    }

    private fun createPanoptesUser() {
        println("create default user")
        val panoptesUser = JsonObject().put("email", "panoptes").put("password", "panoptes")
        client.find(Constants.MONGO_COLLECTION_USERS,panoptesUser,{panoptesUserFind ->
            if (panoptesUserFind.succeeded() && !panoptesUserFind.result().isEmpty()){
                println("default user already exist")
                suscribeCommand()
            } else {
                client.save(Constants.MONGO_COLLECTION_USERS, panoptesUser, { userCreation ->
                    if (userCreation.succeeded()){
                        println("default user created")
                        suscribeCommand()
                    } else {
                        println("cannot create panoptes user  so kill ${this.javaClass.name} is dieing")
                    }
                } )
            }
        })

    }


}

