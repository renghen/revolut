package com.revolut.accounts.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.revolut.accounts.models.AccountDetails
import com.revolut.accounts.models.Bank
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

data class AccountSummary(val accountNumber: String, val accountDetails: AccountDetails,
                          @JsonSerialize(using = CustomDoubleSerializer::class)
                          val balance: Double)

data class AccountCreation(val accountDetails: AccountDetails, val balance: Double)

const val AccountNotFound = """{message : "account number not found"}"""
const val BadRequest = """{message : "bad request"}"""

val accountCreationLens = Body.auto<AccountCreation>().toLens()

fun bankApp(bank: Bank): RoutingHttpHandler =
        "/bank" bind routes(
                "/details" bind GET to {
                    Response(OK).body("{bank : ${bank.name}}")
                },
                "/accountsLeft" bind GET to {
                    Response(OK).body("{accountsLeft : ${bank.getAccountAvailable()}}")
                },
                "/accountList" bind GET to {
                    val accounts = bank.getAccounts().map {
                        AccountSummary(it.accountNumber, it.accountDetails, it.balance())
                    }
                    val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(accounts)
                    Response(OK).body(serialized)
                },
                "/account/{accountNumber}" bind GET to { req: Request ->
                    val accountNumber = req.path("accountNumber")
                    if (accountNumber == null) {
                        Response(BAD_REQUEST).body(BadRequest)
                    } else {
                        val account = bank[accountNumber]
                        if (account == null) {
                            Response(NOT_FOUND).body(AccountNotFound)
                        } else {
                            val accountSummary = AccountSummary(account.accountNumber, account.accountDetails, account.balance())
                            val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(accountSummary)
                            Response(OK).body(serialized)
                        }
                    }
                },
                "/account" bind POST to { req: Request ->
                    val accountCreation = try {
                        accountCreationLens(req)
                    } catch (e: Exception) {
                        null
                    }
                    if (accountCreation == null) {
                        Response(BAD_REQUEST).body(BadRequest)
                    } else {
                        val accountNumber = bank.createAccount(accountCreation.accountDetails,accountCreation.balance)
                        Response(OK).body("""{accountNumber:$accountNumber}""")
                    }
                }

        )

