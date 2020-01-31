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
    fun `add incorrect amount`() {
        val account = bank["0005"]
        if (account != null) {
            assertFailsWith(IllegalArgumentException::class) {
                account.addMoney(-1.0)
            }
        } else {
            assertNotNull<Account>(account, "account '0005' cannot be null")
        }
    }

    @Test
    fun `remove incorrect amount`() {
        val account = bank["0005"]
        if (account != null) {
            assertFailsWith(IllegalArgumentException::class) {
                account.removeMoney(-1.0)
            }
        } else {
            assertNotNull<Account>(account, "account '0005' cannot be null")
        }
    }

    @Test
    fun `remove too much`() {
        val account = bank["0005"]
        if (account != null) {
            assertFailsWith(NotEnoughMoneyException::class) {
                try {
                    account.removeMoney(account.balance() + 10)
                } catch (e: Exception) {
                    throw e.cause ?: e
                }
            }
        } else {
            assertNotNull<Account>(account, "account '0005' cannot be null")
        }
    }

    @Test
    fun `increment account by 1 for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val account = bank["0000"]

        if (account != null) {
            val futures = (1..1000).map {
                CompletableFuture.supplyAsync(Supplier {
                    account.addMoney(1.toDouble())
                }, executorService)
            }.toTypedArray()

            CompletableFuture.allOf(*futures).get()
            assertEquals(1100.00, account.balance())
        } else {
            assertNotNull<Account>(account, "account '0000' cannot be null")
        }
    }


    @Test
    fun `transfers account for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0010"]
        val accountB = bank["0011"]

        if (accountA != null && accountB != null) {
            val balanceA = accountA.balance()
            val futures = (1..1000).map {
                CompletableFuture.supplyAsync(Supplier {
                    if (it % 2 == 0) {
                        accountA.transferTo(accountB, 1.toDouble())
                    } else {
                        accountB.transferTo(accountA, 1.toDouble())
                    }
                }, executorService)
            }.toTypedArray()

            CompletableFuture.allOf(*futures).get()
            assertEquals(balanceA, accountA.balance())
        } else {
            assertNotNull<Account>(accountA, "account '0010' cannot be null")
            assertNotNull<Account>(accountB, "account '0011' cannot be null")
        }
    }

}