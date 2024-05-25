    package com.khodchenko.plugins

    import io.ktor.http.*
    import io.ktor.server.application.*
    import io.ktor.server.auth.*
    import io.ktor.server.request.*
    import io.ktor.server.response.*
    import io.ktor.server.routing.*
    import io.ktor.server.sessions.*
    import java.io.File

    fun Application.configureRouting() {
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

            get("/logout") {
                call.sessions.clear<UserSession>()
                call.respondRedirect("/login")
            }

            authenticate {
                get("/") {
                    call.respondText("Welcome to the protected page!")
                }
            }
        }
    }
