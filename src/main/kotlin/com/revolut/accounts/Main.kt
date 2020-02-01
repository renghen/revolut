package com.revolut.accounts

import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import com.revolut.accounts.controllers.*
import com.revolut.accounts.utils.BankUtils
import org.http4k.routing.routes

val bank = BankUtils.`concurrent creation of N Accounts`(100, name = "ABC")

fun bankServer(port: Int): Http4kServer =
        routes(bankApp(bank), accountApp(bank)).asServer(Jetty(port))

fun main(args: Array<String>) {
    bankServer(9000).start()
}
