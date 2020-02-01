package com.revolut.accounts.utils

import com.revolut.accounts.models.AccountDetails
import com.revolut.accounts.models.Bank
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier

object BankUtils {
    fun `concurrent creation of N Accounts`(n: Int, initialAmount: Double = 100.00, name: String = "abc"): Bank {
        val bank = Bank(name)
        val executorService = Executors.newFixedThreadPool(10)
        val futures = (1..n).map {
            CompletableFuture.supplyAsync(Supplier {
                val accountDetails = AccountDetails(it.toString())
                bank.createAccount(accountDetails, initialAmount)
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        return bank
    }
}