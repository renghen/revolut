package com.revolut.accounts.models

import org.junit.Test
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.test.*


class BankTest {

    companion object {
        fun `concurrent creation of N Accounts`(n: Int): Bank {
            val bank = Bank("abc")
            val executorService = Executors.newFixedThreadPool(10)
            val futures = (1..n).map {
                CompletableFuture.supplyAsync(Supplier {
                    val accountDetails = AccountDetails(it.toString())
                    bank.createAccount(accountDetails, 100.00)
                }, executorService)
            }.toTypedArray()

            CompletableFuture.allOf(*futures).get()
            return bank
        }
    }

    @Test
    fun `bank creation`() {
        val bank = Bank("abc")
        assertEquals(bank.getAccountAvailable(), 1000)
    }

    @Test
    fun `100 account creation`() {
        val bank = Bank("abc")
        for (i in 1..100) {
            val accountDetails = AccountDetails(i.toString())
            bank.createAccount(accountDetails, 100.00)
        }
        assertEquals(900, bank.getAccountAvailable())
    }

    @Test
    fun `100 account creation concurrent`() {
        val bank = `concurrent creation of N Accounts`(100)
        assertEquals(900, bank.getAccountAvailable())
    }

    @Test
    fun `100 account creation concurrent with same initial amount`() {
        val bank = `concurrent creation of N Accounts`(100)
        val accounts = bank.getAccounts()
        var i = 0
        accounts.forEach {
            val expectedAccountNumber = i.toString().padStart(4, '0')
            i += 1
            assertEquals(expectedAccountNumber, it.accountNumber)
            assertEquals(100.00, it.balance())
        }
    }

    @Test
    fun `1001 account creation concurrent should produce an exception`() {
        assertFailsWith(AccountFullException::class) {
            try {
                `concurrent creation of N Accounts`(1001)
            } catch (e: Exception) {
                println(e.cause)
                throw e.cause?.cause ?: e
            }
        }
    }

    @Test
    fun `1001 account creation concurrent should still have 1000 accounts,even with exception`() {
        val bank: Bank = `concurrent creation of N Accounts`(1000)
        try {
            bank.createAccount(AccountDetails("1001"), 100.0)
        } catch (e: Exception) {
            println(e.cause)
        }
        assertEquals(1000, bank.getAccounts().size)
    }
}


