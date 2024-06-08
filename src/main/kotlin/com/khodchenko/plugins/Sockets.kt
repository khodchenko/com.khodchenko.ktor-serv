import com.khodchenko.plugins.*
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
    val userService = UserService(connectToMongoDB())


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


            val existingMessages = messageService.findByRoomId(roomId)
            existingMessages.forEach { message ->
                session.send(Frame.Text(Json.encodeToString(message)))
            }

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val user = call.sessions.get<UserSession>()
                        val receivedText = frame.readText()
                        val userId = user?.userId ?: "Unknown"
                        val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                        val userNickname = userService.read(userId)?.nickname ?: "Unknown"
                        val message = Message(roomId, userId, userNickname, receivedText, timestamp)

                        // Сохранить сообщение в базу данных
                        messageService.create(message)

                        // Отправить сообщение всем клиентам в комнате
                        rooms[roomId]?.forEach { otherSession ->
                            otherSession.send(Frame.Text(Json.encodeToString(message)))
                        }
                    }
                }
            } finally {
                rooms[roomId]?.remove(session)
            }
        }
    }
}

