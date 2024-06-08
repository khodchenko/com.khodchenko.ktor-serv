    package com.khodchenko.plugins

    import io.ktor.http.*
    import io.ktor.server.application.*
    import io.ktor.server.auth.*
    import io.ktor.server.http.content.*
    import io.ktor.server.request.*
    import io.ktor.server.response.*
    import io.ktor.server.routing.*
    import io.ktor.server.sessions.*
    import java.io.File
    import java.time.Instant
    import java.time.format.DateTimeFormatter

    fun Application.configureRouting() {
        val userService = UserService(connectToMongoDB())
        val roomService = RoomService(connectToMongoDB())
        val messageService = MessageService(connectToMongoDB())

        routing {
            staticResources("/static", "static")

            get("/login") {
                call.respondFile(File("src/main/resources/static/login.html"))
            }

            get("/register") {
                call.respondFile(File("src/main/resources/static/register.html"))
            }

            post("/login") {
                val postParameters = call.receiveParameters()
                val email = postParameters["email"]?.lowercase() ?: return@post call.respondText(
                    "Missing email",
                    status = HttpStatusCode.Unauthorized
                )
                val password = postParameters["password"] ?: return@post call.respondText(
                    "Missing password",
                    status = HttpStatusCode.Unauthorized
                )

                val user = userService.findByUsername(email)
                if (user != null && user.password == password) {
                    call.sessions.set(UserSession(userId = user.id!!))
                    call.respondRedirect("/")
                } else {
                    call.respondRedirect("/login?error=Invalid+credentials")
                }
            }

            post("/register") {
                val postParameters = call.receiveParameters()
                val username =
                    postParameters["username"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing username")
                val password =
                    postParameters["password"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing password")
                val confirmPassword = postParameters["confirm-password"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing confirm password"
                )
                val nickname =
                    postParameters["nickname"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing nickname")
                val avatar =
                    postParameters["avatar"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing avatar")

                if (password != confirmPassword) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Passwords do not match")
                }

                val existingUser = userService.findByUsername(username)
                if (existingUser != null) {
                    return@post call.respond(HttpStatusCode.Conflict, "That email is already registered")
                }

                val registrationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                val newUser = User(
                    username = username,
                    password = password,
                    nickname = nickname,
                    avatar = avatar,
                    registrationDate = registrationDate
                )
                val userId = userService.create(newUser)
                call.sessions.set(UserSession(userId = userId))
                call.respondRedirect("/")
            }

            get("/rooms") {
                call.respondFile(File("src/main/resources/static/rooms.html"))
            }

            get("/create-room") {
                call.respondFile(File("src/main/resources/static/createroom.html"))
            }

            post("/create-room") {
                val postParameters = call.receiveParameters()
                val roomName = postParameters["roomName"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing roomName")
                val password = postParameters["password"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing password")
                val playerCount = postParameters["playerCount"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid playerCount")

                val creationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                val newRoom = Room(
                    roomName = roomName,
                    password = password,
                    hostId = call.sessions.get<UserSession>()?.userId.toString(),
                    creationDate = creationDate,
                    playerCount = playerCount
                )
                val roomId = roomService.create(newRoom)
                call.respondRedirect("/room/$roomId")
            }

            get("/rooms-data") {
                val rooms = roomService.findAll()
                call.respond(rooms)
            }

            get("/room/{id}") {
                call.respondFile(File("src/main/resources/static/room.html"))
            }

            get("/room-data/{id}") {
                val roomId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid room ID")
                val room = roomService.read(roomId) ?: return@get call.respond(HttpStatusCode.NotFound, "Room not found")
                call.respond(room)
            }

            post("/join-room/{id}") {
                val roomId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid room ID")
                val userSession = call.sessions.get<UserSession>() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val user = userService.read(userSession.userId) ?: return@post call.respond(HttpStatusCode.NotFound, "User not found")

                val added = roomService.addPlayerToRoom(roomId, user.username)
                if (added) {
                    call.respond(HttpStatusCode.OK, "Joined room successfully")
                } else {
                    call.respond(HttpStatusCode.Conflict, "Unable to join the room")
                }
            }

            post("/leave-room/{id}") {
                val roomId = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing or invalid room ID")
                val userSession = call.sessions.get<UserSession>() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val user = userService.read(userSession.userId) ?: return@post call.respond(HttpStatusCode.NotFound, "User not found")

                val removed = roomService.removePlayerFromRoom(roomId, user.username)
                if (removed) {
                    call.respond(HttpStatusCode.OK, "Left room successfully")
                } else {
                    call.respond(HttpStatusCode.Conflict, "Unable to leave the room")
                }
            }

            delete("/delete-room/{id}") {
                val roomId = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing or invalid room ID")
                val room = roomService.read(roomId) ?: return@delete call.respond(HttpStatusCode.NotFound, "Room not found")
                val userSession = call.sessions.get<UserSession>() ?: return@delete call.respond(HttpStatusCode.Unauthorized)

                if (room.hostId != userSession.userId) {
                    return@delete call.respond(HttpStatusCode.Forbidden, "You are not authorized to delete this room")
                }
                messageService.deleteByRoomId(roomId)
                roomService.delete(roomId)
                call.respond(HttpStatusCode.NoContent)
            }

            get("/logout") {
                call.sessions.clear<UserSession>()
                call.respondRedirect("/login")
            }

            authenticate {
                get("/") {
                    call.respondFile(File("src/main/resources/static/index.html"))
                }

                get("/user-data") {
                    val userSession = call.sessions.get<UserSession>()
                    if (userSession == null) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }

                    val userId = userSession.userId
                    val user =
                        userService.read(userId) ?: return@get call.respond(HttpStatusCode.NotFound, "User not found")

                    call.respond(
                        mapOf(
                            "username" to user.username,
                            "email" to user.username, // Assuming username is email
                            "nickname" to user.nickname,
                            "avatar" to user.avatar,
                            "registrationDate" to user.registrationDate
                        )
                    )
                }
            }
        }
    }
