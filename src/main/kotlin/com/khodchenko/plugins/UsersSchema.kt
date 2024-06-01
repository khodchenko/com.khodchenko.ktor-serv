package com.khodchenko.plugins

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.bson.Document
import org.bson.types.ObjectId


@Serializable
data class User(
    val username: String,
    val password: String,
    val nickname: String,
    val avatar : String,
    val registrationDate : String
) {
    fun toDocument(): Document = Document.parse(Json.encodeToString(this))

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): User = json.decodeFromString(document.toJson())
    }
}

class UserService(database: MongoDatabase) {
    private var collection: MongoCollection<Document>

    init {
        collection = database.getCollection("users")
    }

    // Create new user
    suspend fun create(user: User): String = withContext(Dispatchers.IO) {
        val doc = user.toDocument()
        collection.insertOne(doc)
        doc["_id"].toString()
    }

    // Read a user
    suspend fun read(id: String): User? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("_id", ObjectId(id))).first()?.let(User::fromDocument)
    }

    // Find user by username
    suspend fun findByUsername(username: String): User? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("username", username)).first()?.let(User::fromDocument)
    }

    // Update a user
    suspend fun update(id: String, user: User): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndReplace(Filters.eq("_id", ObjectId(id)), user.toDocument())
    }

    // Delete a user
    suspend fun delete(id: String): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndDelete(Filters.eq("_id", ObjectId(id)))
    }
}
