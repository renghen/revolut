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
        var str= ""
        StmUtils.atomic(Runnable {
            val current = currentAccount.atomicIncrementAndGet(1)
            if (current >= maxAccount) {
                throw AccountFullException()
            }
            str = current.toString().padStart(4, '0')
        })
        return str
    }

    @Throws(AccountFullException::class)
    fun createAccount(accountDetails: AccountDetails, initialAmount: Double) {
        StmUtils.atomic(Runnable {
            val acc = Account.createAccount(accountDetails, initialAmount, this)
            accountsMap[acc.accountNumber] = acc
        })
    }

    fun getAccount(accountNumber :String) : Account?{
        return accountsMap[accountNumber]
    }

    operator fun get(accountNumber: String) : Account?{
        return accountsMap[accountNumber]
    }

}