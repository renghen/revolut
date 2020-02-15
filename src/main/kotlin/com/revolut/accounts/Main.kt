package com.revolut.accounts

import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import com.revolut.accounts.controllers.*
import com.revolut.accounts.models.Bank
import com.revolut.accounts.models.FixedFee
import com.revolut.accounts.models.PercentageFee
import com.revolut.accounts.utils.BankUtils
import org.http4k.routing.routes
import java.util.concurrent.ConcurrentHashMap

object Main {
    val banks = ConcurrentHashMap<String, Bank>()

    init {
        val bankA = BankUtils.`concurrent creation of N Accounts`(100, name = "ABC")
        val bankB = BankUtils.`concurrent creation of N Accounts`(100, name = "XYZ")
        bankA.addForeignBank(bankB, FixedFee(1.0))
        bankB.addForeignBank(bankA, PercentageFee(5.0))
        banks[bankA.name] = bankA
        banks[bankB.name] = bankB
    }

    fun bankServer(port: Int): Http4kServer =
            routes(bankApp(banks), accountApp(banks)).asServer(Jetty(port))
}

fun main(args: Array<String>) {
    Main.bankServer(9000).start()
}
