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

const val NotEnoughMoney = """{message : "account number has not enough money"}"""
const val MoneyParameter = """{message : "money parameter cannot be negative"}"""
const val UnknownErrorMsg = """{message : "Unknown Error"}"""

fun accountApp(bank: Bank): RoutingHttpHandler =
        "/account" bind routes(
                "/addMoney" bind Method.PUT to { req ->
                    val accountBalanceManipulation = try {
                        accountBalanceManipulationLens(req)
                    } catch (e: Exception) {
                        null
                    }
                    if (accountBalanceManipulation == null) {
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
                },
                "/removeMoney" bind Method.PUT to { req ->
                    val accountBalanceManipulation = try {
                        accountBalanceManipulationLens(req)
                    } catch (e: Exception) {
                        null
                    }

                    if (accountBalanceManipulation == null) {
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
                /*,
                "/removeMoney bind Method.PUT to {
                    Response(Status.OK).body("{accountsLeft : ${bank.getAccountAvailable()}}")
                },
                "/transfer" bind Method.POST to { req: Request ->
                    val accountCreation = try {
                        accountCreationLens(req)
                    } catch (e: Exception) {
                        null
                    }
                    if (accountCreation == null) {
                        Response(Status.BAD_REQUEST).body(BadRequest)
                    } else {
                        val accountNumber = bank.createAccount(accountCreation.accountDetails,accountCreation.balance)
                        Response(Status.OK).body("""{accountNumber:$accountNumber}""")
                    }
                }*/

        )