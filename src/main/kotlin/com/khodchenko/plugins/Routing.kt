package com.khodchenko.plugins

import com.mongodb.client.MongoDatabase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

fun Application.configureRouting() {
    val userService = UserService(connectToMongoDB())

    routing {
        // Отправка существующих страниц login.html и register.html
        get("/login") {
            call.respondFile(File("src/main/resources/static/login.html"))
        }

        get("/register") {
            call.respondFile(File("src/main/resources/static/register.html"))
        }

        post("/login") {
            val postParameters = call.receiveParameters()
            val login = postParameters["login"] ?: return@post call.respondText("Missing login", status = HttpStatusCode.Unauthorized)
            val password = postParameters["password"] ?: return@post call.respondText("Missing password", status = HttpStatusCode.Unauthorized)

            // Пример проверки (замените на вашу логику проверки пользователя)
            if (login == "test@example.com" && password == "password") {
                call.sessions.set(UserSession(userId = "some_user_id"))
                call.respondRedirect("/")
            } else {
                call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
            }
        }

        post("/register") {
            val postParameters = call.receiveParameters()
            val username = postParameters["username"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing username")
            val password = postParameters["password"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing password")
            val nickname = postParameters["nickname"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing nickname")
            val avatar = postParameters["avatar"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing avatar")
            val existingUser = userService.findByUsername(username)
            if (existingUser != null) {
                return@post call.respond(HttpStatusCode.Conflict, "That email is already registered")
            }

            val registrationDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            val newUser = User(username, password, nickname, avatar, registrationDate)
            val userId = userService.create(newUser)
            call.sessions.set(UserSession(userId = userId))
            call.respondRedirect("/")
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
                val user = userService.read(userId) ?: return@get call.respond(HttpStatusCode.NotFound, "User not found")

                call.respond(mapOf(
                    "username" to user.username,
                    "email" to user.username,  // Assuming username is email
                    "nickname" to user.nickname,
                    "avatar" to user.avatar,
                    "registrationDate" to user.registrationDate
                ))
            }
        }
    }
}
