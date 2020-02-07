package com.revolut.accounts.controllers

import com.revolut.accounts.models.*
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

data class AccountBalanceManipulation(val accountNumber: String, val amount: Double)

val accountBalanceManipulationLens = Body.auto<AccountBalanceManipulation>().toLens()

data class AccountTransfer(val accountNumberA: String, val accountNumberB: String, val amount: Double)

val accountTransferLens = Body.auto<AccountTransfer>().toLens()

data class AccountTransferInterBankAccount(val accountNumberA: String, val bankName :String,val accountNumberB: String,
                                           val amount: Double)

val accountTransferInterBankAccountLens = Body.auto<AccountTransferInterBankAccount>().toLens()

const val NotEnoughMoney = """{message : "account number has not enough money"}"""
const val MoneyParameter = """{message : "money parameter cannot be negative"}"""
const val UnknownErrorMsg = """{message : "Unknown Error"}"""

fun accountApp(banks: ConcurrentHashMap<String, Bank>): RoutingHttpHandler =
        "/account" bind routes(
                "/{bankName}/addMoney" bind Method.PUT to { req ->
                    requestBankValidation(req, banks, ::addMoneyToAccount)
                },
                "/{bankName}/removeMoney" bind Method.PUT to { req ->
                    requestBankValidation(req, banks, ::removeMoneyFromAccount)
                },
                "/{bankName}/transfer" bind Method.PUT to { req ->
                    requestBankValidation(req, banks, ::transferMoneyBetweenAccounts)
                },
                "/{bankName}/transferInterbank" bind Method.PUT to { req ->
                    requestBankValidation(req, banks, ::transferMoneyBetweenAccounts)
                }
        )

fun transferMoneyBetweenBankAccounts(req: Request, bank: Bank): Response {
    val transferParams = try {
        accountTransferInterBankAccountLens(req)
    } catch (e: Exception) {
        null
    }

    return if (transferParams == null) {
        Response(Status.BAD_REQUEST).body(BadRequest)
    } else {
        val accountA = bank[transferParams.accountNumberA]
        if (accountA == null) {
            Response(Status.BAD_REQUEST).body(AccountNotFound)
        } else {
            try {
                accountA.transferToAccountInOtherBank(transferParams.bankName,transferParams.accountNumberB,transferParams.amount)
                val msg = """"{message" : "transfer successful}"""
                Response(Status.OK).body(msg)
            } catch (ex: Exception) {
                when (ex) {
                    is IllegalArgumentException -> {
                        Response(Status.BAD_REQUEST).body(MoneyParameter)
                    }
                    is NotEnoughMoneyException -> {
                        Response(Status.BAD_REQUEST).body(NotEnoughMoney)
                    }
                    is TransferFeeDoesNotExistException ->{
                        Response(Status.BAD_REQUEST).body(ForeignBankFeeNotFound)
                    }
                    is AccountNotFoundException ->{
                        Response(Status.BAD_REQUEST).body(OtherAccountNotFound)
                    }
                    else -> {
                        Response(Status.INTERNAL_SERVER_ERROR).body(UnknownErrorMsg)
                    }
                }
            }
        }
    }
}


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

fun transferMsg(balanceA: Double, balanceB: Double) = """
    {
       "message" : "transfer successfull",
       "balanceA" : $balanceA,
       "balanceB" : $balanceB
    }    
""".trimIndent()

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