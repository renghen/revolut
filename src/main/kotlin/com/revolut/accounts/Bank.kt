package com.revolut.accounts

import org.multiverse.api.StmUtils
import org.multiverse.api.references.TxnLong

class AccountFullException : Exception("Account Full")

class Bank constructor(val name :String){

    private val maxAccount: Long = 1000
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

}