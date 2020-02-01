package com.revolut.accounts

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun BankServer(port: Int): Http4kServer = { _: Request -> Response(Status.OK) }.asServer(Jetty(port))

fun main(args: Array<String>) {
   println("hello world")
}
