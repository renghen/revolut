package com.revolut.accounts.controllers

import com.revolut.accounts.models.*
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.lang.Exception

data class AccountBalanceManipulation(val accountNumber: String, val amount: Double)

val accountBalanceManipulationLens = Body.auto<AccountBalanceManipulation>().toLens()

data class AccountTransfer(val accountNumberA: String, val accountNumberB: String, val amount: Double)

val accountTransferLens = Body.auto<AccountTransfer>().toLens()

const val NotEnoughMoney = """{message : "account number has not enough money"}"""
const val MoneyParameter = """{message : "money parameter cannot be negative"}"""
const val UnknownErrorMsg = """{message : "Unknown Error"}"""

fun accountApp(bank: Bank): RoutingHttpHandler =
        "/account" bind routes(
                "/addMoney" bind Method.PUT to { req ->
                    addMoneyToAccount(req, bank)
                },
                "/removeMoney" bind Method.PUT to { req ->
                    removeMoneyFromAccount(req, bank)

                },
                "/transfer" bind Method.PUT to { req ->
                    transferMoneyBetweenAccounts(req, bank)
                }
        )

fun transferMoneyBetweenAccounts(req: Request, bank: Bank): Response {
    val accountTransfer = try {
        accountTransferLens(req)
    } catch (e: Exception) {
        null
    }

    return if (accountTransfer == null) {
        Response(Status.BAD_REQUEST).body(BadRequest)
    } else {
        val accountA = bank[accountTransfer.accountNumberA]
        val accountB = bank[accountTransfer.accountNumberB]
        if (accountA == null || accountB == null) {
            Response(Status.BAD_REQUEST).body(AccountNotFound)
        } else {
            try {
                accountA.transferTo(accountB, accountTransfer.amount)
                val msg = transferMsg(accountA.balance(), accountB.balance())
                Response(Status.OK).body(msg)
            } catch (ex: Exception) {
                when (ex) {
                    is IllegalArgumentException -> {
                        Response(Status.BAD_REQUEST).body(MoneyParameter)
                    }
                    is NotEnoughMoneyException -> {
                        Response(Status.BAD_REQUEST).body(NotEnoughMoney)
                    }
                    else -> {
                        Response(Status.INTERNAL_SERVER_ERROR).body(UnknownErrorMsg)
                    }
                }
            }

        }
    }
}

fun removeMoneyFromAccount(req: Request, bank: Bank): Response {
    val accountBalanceManipulation = try {
        accountBalanceManipulationLens(req)
    } catch (e: Exception) {
        null
    }

    return if (accountBalanceManipulation == null) {
        Response(Status.BAD_REQUEST).body(BadRequest)
    } else {
        val account = bank[accountBalanceManipulation.accountNumber]
        if (account == null) {
            Response(Status.BAD_REQUEST).body(AccountNotFound)
        } else {
            try {
                val balance = account.removeMoney(accountBalanceManipulation.amount)
                Response(Status.OK).body("""{balance: $balance}""")
            } catch (ex: Exception) {
                when (ex) {
                    is IllegalArgumentException -> {
                        Response(Status.BAD_REQUEST).body(MoneyParameter)
                    }
                    is NotEnoughMoneyException -> {
                        Response(Status.BAD_REQUEST).body(NotEnoughMoney)
                    }
                    else -> {
                        Response(Status.INTERNAL_SERVER_ERROR).body(UnknownErrorMsg)
                    }
                }
            }

        }
    }
}

fun addMoneyToAccount(req: Request, bank: Bank): Response {
    val accountBalanceManipulation = try {
        accountBalanceManipulationLens(req)
    } catch (e: Exception) {
        null
    }
    return if (accountBalanceManipulation == null) {
        Response(Status.BAD_REQUEST).body(BadRequest)
    } else {
        val account = bank[accountBalanceManipulation.accountNumber]
        if (account == null) {
            Response(Status.BAD_REQUEST).body(AccountNotFound)
        } else {
            try {
                val balance = account.addMoney(accountBalanceManipulation.amount)
                Response(Status.OK).body("""{balance: $balance}""")

            } catch (ex: Exception) {
                when (ex) {
                    is IllegalArgumentException -> {
                        Response(Status.BAD_REQUEST).body(MoneyParameter)
                    }
                    else -> {
                        Response(Status.INTERNAL_SERVER_ERROR).body(UnknownErrorMsg)
                    }
                }
            }

        }
    }
}

fun transferMsg(balanceA: Double, balanceB: Double) = """
    {
       "message" : "transfer successfull",
       "balanceA" : $balanceA,
       "balanceB" : $balanceB
    }    
""".trimIndent()