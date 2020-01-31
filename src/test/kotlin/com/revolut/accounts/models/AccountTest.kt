package com.revolut.accounts.models

import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.test.*

class AccountTest {

    companion object{
        val bank = BankTest.`concurrent creation of N Accounts`(100)
    }

    

}