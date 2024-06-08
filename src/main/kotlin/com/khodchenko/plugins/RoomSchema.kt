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
data class Room(
    val roomName: String,
    val password: String,
    val hostId: String,
    val creationDate: String,
    val playerCount: Int,
    val players: MutableList<String> = mutableListOf()
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

        fun fromDocument(document: Document): Room {
            val room = json.decodeFromString<Room>(document.toJson())
            room.id = document.getObjectId("_id").toString()
            return room
        }
    }
}

class RoomService(database: MongoDatabase) {
    private var collection: MongoCollection<Document>

    init {
        collection = database.getCollection("rooms")
    }

    // Create new room
    suspend fun create(room: Room): String = withContext(Dispatchers.IO) {
        val doc = room.toDocument()
        collection.insertOne(doc)
        doc.getObjectId("_id").toString()
    }

    // Read a room
    suspend fun read(id: String): Room? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("_id", ObjectId(id))).first()?.let(Room::fromDocument)
    }

    // Find room by roomName
    suspend fun findByRoomName(roomName: String): Room? = withContext(Dispatchers.IO) {
        collection.find(Filters.eq("roomName", roomName)).first()?.let(Room::fromDocument)
    }

    // Add a player to a room
    suspend fun addPlayerToRoom(roomId: String, username: String): Boolean = withContext(Dispatchers.IO) {
        val room = read(roomId)
        if (room == null) {
            println("Room not found: $roomId")
            return@withContext false
        }
        if (room.players.contains(username)) {
            println("User already in room: $username")
            return@withContext false
        }
        if (room.players.size >= room.playerCount) {
            println("Room is full: $roomId")
            return@withContext false
        }
        room.players.add(username)
        update(roomId, room)
        println("User $username added to room $roomId")
        return@withContext true
    }

    // Remove a player from a room
    suspend fun removePlayerFromRoom(roomId: String, username: String): Boolean = withContext(Dispatchers.IO) {
        val room = read(roomId)
        if (room != null && room.players.contains(username)) {
            room.players.remove(username)
            update(roomId, room)
            return@withContext true
        }
        return@withContext false
    }

    // Find all rooms
    suspend fun findAll(): List<Room> = withContext(Dispatchers.IO) {
        collection.find().map { Room.fromDocument(it) }.toList()
    }

    // Update a room
    suspend fun update(id: String, room: Room): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndReplace(Filters.eq("_id", ObjectId(id)), room.toDocument())
    }

    // Delete a room
    suspend fun delete(id: String): Document? = withContext(Dispatchers.IO) {
        collection.findOneAndDelete(Filters.eq("_id", ObjectId(id)))
    }
}
