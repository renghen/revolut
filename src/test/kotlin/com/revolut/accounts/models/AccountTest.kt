package com.revolut.accounts.models

import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.test.*

class AccountTest {

    companion object {
        val bank = BankTest.`concurrent creation of N Accounts`(100)
    }

    @Test
    fun `increment account by 1 for 100 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val account = bank["0000"]

        if (account != null) {
            val futures = (1..100).map {
                CompletableFuture.supplyAsync(Supplier {
                    account.addMoney(1.0)
                }, executorService)
            }.toTypedArray()

            CompletableFuture.allOf(*futures).get()
            assertEquals(200.00, account.balance())
        } else {
            assertNotNull(account, "account '0000' cannot be null")
        }
    }

}