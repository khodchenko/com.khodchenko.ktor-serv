package com.khodchenko.plugins

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.types.ObjectId

@Serializable
data class Message(
    val roomId: String,
    val sender: String,
    val senderNickname: String,
    val content: String,
    val timestamp: String
) {
    var id: String? = null

    fun toDocument(): Document {
        val json = Json.encodeToString(this)
        val doc = Document.parse(json)
        id?.let { doc["_id"] = ObjectId(it) }
        return doc
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromDocument(document: Document): Message {
            val message = json.decodeFromString<Message>(document.toJson())
            message.id = document.getObjectId("_id").toString()
            return message
        }
    }
}

class MessageService(database: MongoDatabase) {
    private val collection: MongoCollection<Document>

    init {
        collection = database.getCollection("messages")
    }

    // Сохранить сообщение
    suspend fun create(message: Message): String = withContext(Dispatchers.IO) {
        val doc = message.toDocument()
        collection.insertOne(doc)
        doc.getObjectId("_id").toString()
    }

    // Получить сообщения по roomId
    suspend fun findByRoomId(roomId: String): List<Message> = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("roomId", roomId)).map { Message.fromDocument(it) }.toList()
    }

    // Удалить сообщения по roomId
    suspend fun deleteByRoomId(roomId: String) = withContext(Dispatchers.IO) {
        collection.deleteMany(Filters.eq("roomId", roomId))
    }
}