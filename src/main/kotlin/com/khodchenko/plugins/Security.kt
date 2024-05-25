package com.khodchenko.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

data class UserSession(val userId: String) : Principal

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<UserSession>("USER_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(Authentication) {
        session<UserSession> {
            validate { session ->
                if (session.userId.isNotEmpty()) session else null
            }
            challenge {
                call.respondRedirect("/login")
            }
        }
    }
}
