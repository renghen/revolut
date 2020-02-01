package com.revolut.accounts.http

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.revolut.accounts.bank
import com.revolut.accounts.bankServer
import com.revolut.accounts.controllers.*
import org.http4k.client.OkHttp
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.*


class AccountHttpTest {
    private val client = OkHttp()
    private val server = bankServer(9090)
    private val accountOfInterest = "0000"

    @Before
    fun setup() {
        server.start()
    }

    @After
    fun teardown() {
        server.stop()
    }

    //region addMoney Test
    @Test
    fun `endpoint account add with not bad request`() {
        val accountInput = """{}"""
        val request = Request(PUT, "http://localhost:${server.port()}/account/addMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(BadRequest)))
    }

    @Test
    fun `endpoint account add with not found error`() {
        val accountInput =
                """{
                      "accountNumber": "notfound", 
                      "amount": 10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/addMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(AccountNotFound)))
    }

    @Test
    fun `endpoint account add with wrong money error`() {

        val accountInput =
                """{
                      "accountNumber": "$accountOfInterest", 
                      "amount": -10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/addMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(MoneyParameter)))
    }

    @Test
    fun `endpoint account add with correct params`() {
        val accountInput =
                """{
                      "accountNumber": "$accountOfInterest", 
                      "amount": 10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/addMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(OK).and(hasBody("""{balance: ${bank[accountOfInterest]!!.balance()}}""")))
    }

    //endregion

    //region removeMoney Test

    @Test
    fun `endpoint account remove with not bad request`() {
        val accountInput = """{}"""
        val request = Request(PUT, "http://localhost:${server.port()}/account/removeMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(BadRequest)))
    }

    @Test
    fun `endpoint account remove with not found error`() {
        val accountInput =
                """{
                      "accountNumber": "notfound", 
                      "amount": 10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/removeMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(AccountNotFound)))
    }

    @Test
    fun `endpoint account remove with wrong money error`() {
        val accountInput =
                """{
                      "accountNumber": "$accountOfInterest", 
                      "amount": -10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/removeMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(MoneyParameter)))
    }

    @Test
    fun `endpoint account remove with too much money error`() {
        val accountInput =
                """{
                      "accountNumber": "$accountOfInterest", 
                      "amount": 200.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/removeMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(NotEnoughMoney)))
    }

    @Test
    fun `endpoint account remove with correct params`() {
        val accountInput =
                """{
                      "accountNumber": "$accountOfInterest", 
                      "amount": 10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/removeMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(OK).and(hasBody("""{balance: ${bank[accountOfInterest]!!.balance()}}""")))
    }

    //endregion

    //region transfer Test

    @Test
    fun `endpoint account transfer with not bad request`() {
        val accountInput = """{}"""
        val request = Request(PUT, "http://localhost:${server.port()}/account/removeMoney").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(BadRequest)))
    }

    @Test
    fun `endpoint account transfer with not found error for accountA`() {
        val accountInput =
                """{
                      "accountNumberA": "notfound",
                      "accountNumberB": "00001",
                      "amount": 10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(AccountNotFound)))
    }

    @Test
    fun `endpoint account transfer with not found error for accountB`() {
        val accountInput =
                """{
                      "accountNumberA": "0000",
                      "accountNumberB": "notfound",
                      "amount": 10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(AccountNotFound)))
    }

    @Test
    fun `endpoint account transfer with not found error for accountA & accountB`() {
        val accountInput =
                """{
                      "accountNumberA": "notfound",
                      "accountNumberB": "notfound",
                      "amount": 10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(AccountNotFound)))
    }

    private val accountOfInterestA = "0000"
    private val accountOfInterestB = "0001"

    @Test
    fun `endpoint account transfer with wrong money error`() {
        val accountInput =
                """{
                      "accountNumberA": "$accountOfInterestA",
                      "accountNumberB": "$accountOfInterestB", 
                      "amount": -10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(MoneyParameter)))
    }

    @Test
    fun `endpoint account transfer with too much money error`() {
        val accountInput =
                """{
                      "accountNumberA": "$accountOfInterestA",
                      "accountNumberB": "$accountOfInterestB", 
                      "amount": 200.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountInput)
        val response = client(request)
        assertThat(response, hasStatus(BAD_REQUEST).and(hasBody(NotEnoughMoney)))
    }

    @Test
    fun `endpoint account transfer with correct params`() {
        val accountInput =
                """{
                      "accountNumberA": "$accountOfInterestA",
                      "accountNumberB": "$accountOfInterestB", 
                      "amount": 10.0
                   }
                """
        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountInput)
        val response = client(request)
        val msg = transferMsg(bank[accountOfInterestA]!!.balance(),bank[accountOfInterestB]!!.balance())
        assertThat(response, hasStatus(OK).and(hasBody(msg)))
    }

    //endregion
}

