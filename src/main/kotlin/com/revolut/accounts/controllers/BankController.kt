package com.revolut.accounts.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.revolut.accounts.models.*
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.format.Jackson.auto
import java.io.IOException
import java.lang.Exception
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap

val mapper = jacksonObjectMapper()

class CustomDoubleSerializer : JsonSerializer<Double?>() {
    @Throws(IOException::class, JsonGenerationException::class)
    override fun serialize(value: Double?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (null == value) { //write the word 'null' if there's no value available
            jgen.writeNull()
        } else {
            val pattern = "#######.##"
            val myFormatter = DecimalFormat(pattern)
            val output = myFormatter.format(value)
            jgen.writeNumber(output)
        }
    }
}

data class BankAccountSummary(val bank: String, val accountSummary: List<AccountSummary>)

data class AccountSummary(val accountNumber: String, val accountDetails: AccountDetails,
                          @JsonSerialize(using = CustomDoubleSerializer::class)
                          val balance: Double)

data class AccountCreation(val accountDetails: AccountDetails, val balance: Double)

const val AccountNotFound = """{message : "account number not found"}"""
const val OtherAccountNotFound = """{message : "The foreign account number not found"}"""
const val BadRequest = """{message : "bad request"}"""
const val BankNotFound = """{message : "Bank not found"}"""
const val ForeignBankFeeNotFound = """{message : "Foreign bank fee not found"}"""

val accountCreationLens = Body.auto<AccountCreation>().toLens()

fun requestBankValidation(req: Request, banks: ConcurrentHashMap<String, Bank>, body: (req: Request, bank: Bank) -> Response): Response {
    val bankName = req.path("bankName")
    return if (bankName == null) {
        Response(BAD_REQUEST).body(BadRequest)
    } else {
        val bank = banks.get(bankName)
        if (bank == null) {
            Response(BAD_REQUEST).body(BankNotFound)
        } else {
            body(req, bank)
        }
    }
}


fun bankApp(banks: ConcurrentHashMap<String, Bank>): RoutingHttpHandler =
        "/bank" bind routes(
                "/{bankName}/details" bind GET to { req ->
                    requestBankValidation(req, banks) { _, bank ->
                        Response(OK).body("{bank : ${bank.name}}")
                    }
                },
                "/{bankName}/accountsLeft" bind GET to { req ->
                    requestBankValidation(req, banks) { _, bank ->
                        Response(OK).body("{accountsLeft : ${bank.getAccountAvailable()}}")
                    }
                },
                "/{bankName}/accountList" bind GET to { req ->
                    requestBankValidation(req, banks) { _, bank ->
                        val accounts = bank.getAccounts().map {
                            AccountSummary(it.accountNumber, it.accountDetails, it.balance())
                        }
                        val bankAccountSummary = BankAccountSummary(bank.name, accounts)
                        val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bankAccountSummary)
                        Response(OK).body(serialized)
                    }
                },
                "/{bankName}/account/{accountNumber}" bind GET to { req: Request ->
                    requestBankValidation(req, banks, ::getAccount)
                },
                "/{bankName}/account" bind POST to { req: Request ->
                    requestBankValidation(req, banks, ::createAccount)
                },
                "/{bankName}/{otherBankName}/interbankRate" bind GET to { req: Request ->
                    requestBankValidation(req, banks, ::getInterBankRate)
                }
        )

data class InterBankJson(val rateType: String, val value: Double)

fun interBankRateToJson(interBankFee: InterBankFee): InterBankJson = when (interBankFee) {
    is PercentageFee -> InterBankJson("percentage", interBankFee.amount)
    is FixedFee -> InterBankJson("Fix Fee", interBankFee.amount)
}

private fun getInterBankRate(req: Request, bank: Bank): Response {
    val otherBankName = req.path("otherBankName")
    return if (otherBankName == null) {
        Response(BAD_REQUEST).body(BadRequest)
    } else {
        val fee = bank.getForeignBankFee(otherBankName)
        if (fee == null) {
            Response(NOT_FOUND).body(ForeignBankFeeNotFound)
        } else {
            val interBankRateToJson = interBankRateToJson(fee)
            val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(interBankRateToJson)
            Response(OK).body(serialized)
        }
    }
}

private fun createAccount(req: Request, bank: Bank): Response {
    val accountCreation = try {
        accountCreationLens(req)
    } catch (e: Exception) {
        null
    }
    return if (accountCreation == null) {
        Response(BAD_REQUEST).body(BadRequest)
    } else {
        val accountNumber = bank.createAccount(accountCreation.accountDetails, accountCreation.balance)
        Response(OK).body("""{accountNumber:$accountNumber}""")
    }
}

private fun getAccount(req: Request, bank: Bank): Response {
    val accountNumber = req.path("accountNumber")
    return if (accountNumber == null) {
        Response(BAD_REQUEST).body(BadRequest)
    } else {
        val account = bank[accountNumber]
        if (account == null) {
            Response(NOT_FOUND).body(AccountNotFound)
        } else {
            val accountSummary = AccountSummary(account.accountNumber, account.accountDetails, account.balance())
            val bankAccountSummary = BankAccountSummary(bank.name, listOf(accountSummary))
            val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bankAccountSummary)
            Response(OK).body(serialized)
        }
    }
}

