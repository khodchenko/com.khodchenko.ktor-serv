package com.khodchenko.plugins

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.ktor.server.application.*

fun Application.configureDatabases() {
    val mongoDatabase = connectToMongoDB()

    createCollectionsIfNotExists(mongoDatabase, "users")
    createCollectionsIfNotExists(mongoDatabase, "rooms")
    createCollectionsIfNotExists(mongoDatabase, "messages")
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

fun createCollectionsIfNotExists(database: MongoDatabase, collectionName: String) {
    if (!database.listCollectionNames().contains(collectionName)) {
        database.createCollection(collectionName)
    }
}
