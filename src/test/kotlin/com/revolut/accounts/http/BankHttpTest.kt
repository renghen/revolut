package com.revolut.accounts.http

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.revolut.accounts.bank
import com.revolut.accounts.bankServer
import com.revolut.accounts.controllers.AccountNotFound
import com.revolut.accounts.controllers.AccountSummary
import com.revolut.accounts.controllers.BadRequest
import com.revolut.accounts.controllers.mapper
import org.http4k.client.OkHttp
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class BankHttpTest {
    private val client = OkHttp()
    private val server = bankServer(9090)

    @BeforeEach
    fun setup() {
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.stop()
    }

    @Test
    fun `endpoint bank details`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/details"))
        assertThat(response, hasStatus(OK).and(hasBody("{bank : ${bank.name}}")))
    }

    @Test
    fun `endpoint bank accountsLeft`() {
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/accountsLeft"))
        assertThat(response, hasStatus(OK).and(hasBody("{accountsLeft : ${bank.getAccountAvailable()}}")))
    }

    @Test
    fun `endpoint bank accountList`() {
        val accounts = bank.getAccounts().map {
            AccountSummary(it.accountNumber, it.accountDetails, it.balance())
        }
        val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(accounts)
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/accountList"))
        assertThat(response, hasStatus(OK).and(hasBody(serialized)))
    }

    @Test
    fun `endpoint bank account accountNumber found`() {
        val accountNumber = "0005"
        val account = bank[accountNumber]!!
        val accountSummary = AccountSummary(account.accountNumber, account.accountDetails, account.balance())
        val serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(accountSummary)
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/account/$accountNumber"))
        assertThat(response, hasStatus(OK).and(hasBody(serialized)))
    }

    @Test
    fun `endpoint bank account accountNumber Not found`() {
        val accountNumber = "notFound"
        val response = client(Request(GET, "http://localhost:${server.port()}/bank/account/$accountNumber"))
        assertThat(response, hasStatus(NOT_FOUND).and(hasBody(AccountNotFound)))
    }

    @Test
    fun `endpoint bank account create account`() {
        val accountInput =
                """{
                      "accountDetails" : {
                        "fullName" : "newAccount"
                      },
                      "balance" : 100
                    }""".trimIndent()
        val request = Request(POST, "http://localhost:${server.port()}/bank/account").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(OK).and(hasBody("{accountNumber:${bank.getAccounts().last().accountNumber}}")))
    }

    @Test
    fun `endpoint bank account create account with error`() {
        val accountInput = """{}"""
        val request = Request(POST, "http://localhost:${server.port()}/bank/account").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(BadRequest)))
    }

}

