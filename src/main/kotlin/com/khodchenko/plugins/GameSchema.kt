package com.khodchenko.plugins

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.types.ObjectId

@Serializable
data class Game(
    val gameName: String,
    val password: String,
    val creationDate: String,
    val playerCount: Int,
    val players : List<String> = emptyList(),
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

        fun fromDocument(document: Document): Game {
            val game = json.decodeFromString<Game>(document.toJson())
            game.id = document.getObjectId("_id").toString()
            return game
        }
    }
}

class GameService(database: MongoDatabase) {
    private var collection: MongoCollection<Document>

    init {
        collection = database.getCollection("games")
    }

    // Create new game
    suspend fun create(game: Game): String = withContext(Dispatchers.IO) {
        val doc = game.toDocument()
        collection.insertOne(doc)
        doc.getObjectId("_id").toString()
    }

    // Read a game
    suspend fun read(id: String): Game? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("_id", ObjectId(id))).first()?.let(Game::fromDocument)
    }

    // Find game by gameName
    suspend fun findByGameName(gameName: String): Game? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("gameName", gameName)).first()?.let(Game::fromDocument)
    }

    // Find all games
    suspend fun findAll(): List<Game> = withContext(Dispatchers.IO) {
        collection.find().map { Game.fromDocument(it) }.toList()
    }

    // Update a game
    suspend fun update(id: String, game: Game): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndReplace(Filters.eq("_id", ObjectId(id)), game.toDocument())
    }

    // Delete a game
    suspend fun delete(id: String): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndDelete(Filters.eq("_id", ObjectId(id)))
    }
}
