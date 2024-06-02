package com.khodchenko.plugins

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

        get("/login") {
            call.respondFile(File("src/main/resources/static/login.html"))
        }

        get("/register") {
            call.respondFile(File("src/main/resources/static/register.html"))
        }

        post("/login") {
            val postParameters = call.receiveParameters()
            val email = postParameters["email"]?.lowercase()  ?: return@post call.respondText(
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
