package com.revolut.accounts.models

import okhttp3.internal.wait
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.test.*


class BankTest {

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
            i +=1
            assertEquals(expectedAccountNumber, it.accountNumber)
            assertEquals(100.00, it.balance())
        }
    }


}


