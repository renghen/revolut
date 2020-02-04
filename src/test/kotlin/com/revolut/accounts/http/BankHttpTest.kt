package com.revolut.accounts.http

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.revolut.accounts.Main.banks
import com.revolut.accounts.Main.bankServer
import com.revolut.accounts.controllers.*
import org.http4k.client.OkHttp
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class BankHttpTest {
    private val client = OkHttp()
    private val server = bankServer(9090)
    private val bankNotExist = "bankNotExist"

    @BeforeEach
    fun setup() {
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.stop()
    }

    @Test
    fun `endpoint bank details bad bank name`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/$bankNotExist/details"))
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(BankNotFound)))
    }

    @Test
    fun `endpoint bank details`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/ABC/details"))
        val name = banks["ABC"]!!.name
        assertThat(response, hasStatus(OK).and(hasBody("{bank : $name}")))
    }

    @Test
    fun `endpoint bank accountsLeft`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/ABC/accountsLeft"))
        val bank = banks["ABC"]!!
        assertThat(response, hasStatus(OK).and(hasBody("{accountsLeft : ${bank.getAccountAvailable()}}")))
    }

    @Test
    fun `endpoint bank accountList`() {
        val bank = banks["ABC"]!!
        val accounts = bank.getAccounts().map {
            AccountSummary(it.accountNumber, it.accountDetails, it.balance())
        }
        val bankAccountSummary = BankAccountSummary(bank.name, accounts)
        val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bankAccountSummary)
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/ABC/accountList"))
        assertThat(response, hasStatus(OK).and(hasBody(serialized)))
    }

    @Test
    fun `endpoint bank account accountNumber found`() {
        val bank = banks["ABC"]!!
        val accountNumber = "0005"
        val account = bank[accountNumber]!!
        val accountSummary = AccountSummary(account.accountNumber, account.accountDetails, account.balance())
        val bankAccountSummary = BankAccountSummary(bank.name, listOf(accountSummary))
        val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bankAccountSummary)
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/ABC/account/$accountNumber"))
        assertThat(response, hasStatus(OK).and(hasBody(serialized)))
    }

    @Test
    fun `endpoint bank account accountNumber Not found`() {
        val accountNumber = "notFound"
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/ABC/account/$accountNumber"))
        assertThat(response, hasStatus(NOT_FOUND).and(hasBody(AccountNotFound)))
    }

    @Test
    fun `endpoint bank account create account`() {
        val bank = banks["ABC"]!!
        val accountInput =
                """{
                      "accountDetails" : {
                        "fullName" : "newAccount"
                      },
                      "balance" : 100
                    }""".trimIndent()
        val request = Request(POST, "http://localhost:${server.port()}/bank/ABC/account").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(OK).and(hasBody("{accountNumber:${bank.getAccounts().last().accountNumber}}")))
    }

    @Test
    fun `endpoint bank account create account with error`() {
        val accountInput = """{}"""
        val request = Request(POST, "http://localhost:${server.port()}/bank/ABC/account").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(BadRequest)))
    }

    @Test
    fun `endpoint bank interBank fee does not exist`() {
        val otherBank = "BankNoExist"
        val request = Request(GET, "http://localhost:${server.port()}/bank/ABC/$otherBank/interbankRate")
        val response = client(request)
        assertThat(response, hasStatus(NOT_FOUND).and(hasBody(ForeignBankFeeNotFound)))
    }

    @Test
    fun `endpoint bank interBank fee Fixed Fee`() {
        val bank = banks["ABC"]!!
        val otherBankName = "XYZ"
        val request = Request(GET, "http://localhost:${server.port()}/bank/${bank.name}/$otherBankName/interbankRate")
        val response = client(request)
        val fee = bank.getForeignBankFee(otherBankName)!!
        val interBankRateToJson = interBankRateToJson(fee)
        val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(interBankRateToJson)
        assertThat(response, hasStatus(OK).and(hasBody(serialized)))
    }

    @Test
    fun `endpoint bank interBank fee Percentage Fee`() {
        val bank = banks["XYZ"]!!
        val otherBankName = "ABC"
        val request = Request(GET, "http://localhost:${server.port()}/bank/${bank.name}/$otherBankName/interbankRate")
        val response = client(request)
        val fee = bank.getForeignBankFee(otherBankName)!!
        val interBankRateToJson = interBankRateToJson(fee)
        val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(interBankRateToJson)
        assertThat(response, hasStatus(OK).and(hasBody(serialized)))
    }

}

