import com.khodchenko.plugins.Message
import com.khodchenko.plugins.MessageService
import com.khodchenko.plugins.UserSession
import com.khodchenko.plugins.connectToMongoDB
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

fun Application.configureSockets() {
    val rooms = ConcurrentHashMap<String, MutableList<DefaultWebSocketServerSession>>()
    val messageService = MessageService(connectToMongoDB())

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/{roomId}") {
            val roomId = call.parameters["roomId"] ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No room ID"))
            val session = this
            rooms.computeIfAbsent(roomId) { mutableListOf() }.add(session)

            // Отправить существующие сообщения новому подключенному клиенту
            val existingMessages = messageService.findByRoomId(roomId)
            existingMessages.forEach { message ->
                session.send(Frame.Text(Json.encodeToString(message)))
            }

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val receivedText = frame.readText()
                        val sender = call.sessions.get<UserSession>()?.userId ?: "Unknown"
                        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                        val message = Message(roomId, sender, receivedText, timestamp)

                        // Сохранить сообщение в базу данных
                        messageService.create(message)

                        // Отправить сообщение всем клиентам в комнате
                        rooms[roomId]?.forEach { otherSession ->
                            if (otherSession != session) {
                                otherSession.send(Frame.Text(Json.encodeToString(message)))
                            }
                        }
                    }
                }
            } finally {
                rooms[roomId]?.remove(session)
            }
        }
    }
}
