package com.khodchenko

import com.khodchenko.plugins.*
import configureSockets
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureSockets()
    configureSerialization()
    configureDatabases()
    configureRouting()
}
