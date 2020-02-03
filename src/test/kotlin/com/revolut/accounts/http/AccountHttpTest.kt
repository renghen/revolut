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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.test.assertEquals


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
        fun `http endpoint account transfer with too much money error`() {
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
            val msg = transferMsg(bank[accountOfInterestA]!!.balance(), bank[accountOfInterestB]!!.balance())
            assertThat(response, hasStatus(OK).and(hasBody(msg)))
        }

    private val accountOfInterestC = "0020"
    private val accountOfInterestD = "0021"
    private val accountTransferCtoD =
            """{
                      "accountNumberA": "$accountOfInterestC",
                      "accountNumberB": "$accountOfInterestD", 
                      "amount": 10.0
                   }
                """

    private val accountTransferDtoC =
            """{
                      "accountNumberA": "$accountOfInterestD",
                      "accountNumberB": "$accountOfInterestC", 
                      "amount": 10.0
                   }
                """


        @Test
        fun `endpoint account transfer with correct params 1000 times concurrently`() {
            val executorService = Executors.newFixedThreadPool(10)
            val futures = (1..2).map {
                CompletableFuture.supplyAsync(Supplier {
                    if (it % 2 == 0) {
                        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountTransferCtoD)
                        client(request)
                        Unit
                    } else {
                        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountTransferDtoC)
                        client(request)
                        Unit
                    }
                }, executorService)
            }.toTypedArray()

            CompletableFuture.allOf(*futures).get()
            assertEquals(100.0, bank[accountOfInterestC]!!.balance())
            assertEquals(100.0, bank[accountOfInterestD]!!.balance())
        }

    @Test
    fun `http endpoint transfers account between 2 accounts for 6000 times concurrently with add and remove added into the mix`() {
        val executorService = Executors.newFixedThreadPool(10)
        val futures = (1..6000).map {
            CompletableFuture.supplyAsync(Supplier {
                when (it % 6) {
                    0 -> {
                        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountTransferCtoD)
                        client(request)
                        Unit
                    }
                    1 -> {
                        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountTransferDtoC)
                        client(request)
                        Unit
                    }
                    2 -> {
                        val accountInput =
                                """{
                      "accountNumber": "$accountOfInterestC", 
                      "amount": 10.0
                   }
                """
                        val request = Request(PUT, "http://localhost:${server.port()}/account/addMoney").body(accountInput)
                        client(request)
                        Unit
                    }
                    3 -> {
                        val accountInput =
                                """{
                      "accountNumber": "$accountOfInterestD", 
                      "amount": 10.0
                   }
                """
                        val request = Request(PUT, "http://localhost:${server.port()}/account/addMoney").body(accountInput)
                        client(request)
                        Unit
                    }
                    4 -> {
                        val accountInput =
                                """{
                      "accountNumber": "$accountOfInterestC", 
                      "amount": 10.0
                   }
                """
                        val request = Request(PUT, "http://localhost:${server.port()}/account/removeMoney").body(accountInput)
                        client(request)
                        Unit
                    }
                    5 -> {
                        val accountInput =
                                """{
                      "accountNumber": "$accountOfInterestD", 
                      "amount": 10.0
                   }
                """
                        val request = Request(PUT, "http://localhost:${server.port()}/account/removeMoney").body(accountInput)
                        client(request)
                        Unit
                    }
                }
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(100.0, bank[accountOfInterestC]!!.balance())
        assertEquals(100.0, bank[accountOfInterestD]!!.balance())
    }

    private val accountOfInterestE = "0022"
    private val accountTransferEtoC =
            """{
                      "accountNumberA": "$accountOfInterestE",
                      "accountNumberB": "$accountOfInterestC", 
                      "amount": 1.0
                   }
                """

    private val accountTransferEtoD =
            """{
                      "accountNumberA": "$accountOfInterestE",
                      "accountNumberB": "$accountOfInterestD", 
                      "amount": 1.0
                   }
                """

    @Test
    fun `http endpoint transfers account between 3 accounts for 4000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountAdd2000ToE =
                """{
                      "accountNumber": "$accountOfInterestE", 
                      "amount": 2000.0
                   }
                """
        val requestAdd2000ToE = Request(PUT, "http://localhost:${server.port()}/account/addMoney").body(accountAdd2000ToE)
        client(requestAdd2000ToE)
        val futures = (1..4000).map {
            CompletableFuture.supplyAsync(Supplier {
                when (it % 4) {
                    0 -> {
                        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountTransferCtoD)
                        client(request)
                        Unit
                    }
                    1 -> {
                        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountTransferDtoC)
                        client(request)
                        Unit
                    }
                    2 -> {
                        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountTransferEtoC)
                        client(request)
                        Unit
                    }
                    3 -> {
                        val request = Request(PUT, "http://localhost:${server.port()}/account/transfer").body(accountTransferEtoD)
                        client(request)
                        Unit
                    }
                }
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(100.0 + 1000.0, bank[accountOfInterestC]!!.balance())
        assertEquals(100.0 + 1000.0, bank[accountOfInterestD]!!.balance())
        assertEquals(100.0, bank[accountOfInterestE]!!.balance())

    }

    //endregion
}

