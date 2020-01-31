package com.revolut.accounts.models

import org.multiverse.api.StmUtils
import org.multiverse.api.references.TxnLong
import java.util.concurrent.ConcurrentHashMap

class AccountFullException : Exception("Account Full")

class Bank constructor(val name: String) {

    private val maxAccount: Long = 1000
    private val accountsMap: ConcurrentHashMap<String, Account> = ConcurrentHashMap()
    private val currentAccount: TxnLong = StmUtils.newTxnLong(0)

    //utility to generate number
    @Throws(AccountFullException::class)
    fun generateAccountNumber(): String {
        currentAccount.atomicIncrementAndGet(1)
        if (currentAccount.atomicGet() >= maxAccount) {
            throw AccountFullException()
        }
        return currentAccount.atomicGet().toString().padStart(4, '0')
    }

    @Throws(AccountFullException::class)
    fun createAccount(accountDetails: AccountDetails, initialAmount: Double) {
        val acc = Account.createAccount(accountDetails, initialAmount, this)
        accountsMap[acc.accountNumber] = acc
    }

}