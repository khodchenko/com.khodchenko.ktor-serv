package com.khodchenko.plugins

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureDatabases() {
    val mongoDatabase = connectToMongoDB()
    val userService = UserService(mongoDatabase)
    routing {
        // Create user
        post("/users") {
            val user = call.receive<User>()
            val id = userService.create(user)
            call.respond(HttpStatusCode.Created, id)
        }
        // Read user
        get("/users/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            userService.read(id)?.let { user ->
                call.respond(user)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        // Update user
        put("/users/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            val user = call.receive<User>()
            userService.update(id, user)?.let {
                call.respond(HttpStatusCode.OK)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
        // Delete user
        delete("/users/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("No ID found")
            userService.delete(id)?.let {
                call.respond(HttpStatusCode.OK)
            } ?: call.respond(HttpStatusCode.NotFound)
        }
    }
}

fun Application.connectToMongoDB(): MongoDatabase {
    val user = environment.config.propertyOrNull("db.mongo.user")?.getString()
    val password = environment.config.propertyOrNull("db.mongo.password")?.getString()
    val host = environment.config.propertyOrNull("db.mongo.host")?.getString() ?: "127.0.0.1"
    val port = environment.config.propertyOrNull("db.mongo.port")?.getString() ?: "27017"
    val maxPoolSize = environment.config.propertyOrNull("db.mongo.maxPoolSize")?.getString()?.toInt() ?: 20
    val databaseName = environment.config.propertyOrNull("db.mongo.database.name")?.getString() ?: "servDatabase"

    val credentials = user?.let { userVal -> password?.let { passwordVal -> "$userVal:$passwordVal@" } }.orEmpty()
    val uri = "mongodb://$credentials$host:$port/?maxPoolSize=$maxPoolSize&w=majority"

    val mongoClient = MongoClients.create(uri)
    val database = mongoClient.getDatabase(databaseName)

    environment.monitor.subscribe(ApplicationStopped) {
        mongoClient.close()
    }

    return database
}
