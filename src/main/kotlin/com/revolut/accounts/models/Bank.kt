package com.revolut.accounts.models

import scala.concurrent.stm.japi.STM
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class AccountFullException : Exception("Account Full")

class Bank constructor(val name: String) {

    companion object{
        fun formatIntForAcountNumber(l : Long) : String = l.toString().padStart(4, '0')
    }

    private val maxAccount: Long = 1000
    private val accountsMap: ConcurrentHashMap<String, Account> = ConcurrentHashMap()
    private var currentAccount: Long = -1
    val ledger = STM.newRef<List<AccountAction>>(listOf())

    //utility to generate number
    @Throws(AccountFullException::class)
    @Synchronized
    fun generateAccountNumber(): String {
        currentAccount += 1
        if (currentAccount >= maxAccount) {
            throw AccountFullException()
        }
        return formatIntForAcountNumber(currentAccount)
    }

    @Throws(AccountFullException::class)
    @Synchronized
    fun createAccount(accountDetails: AccountDetails, initialAmount: Double) :String {
        val acc = Account.createAccount(accountDetails, initialAmount, this)
        accountsMap[acc.accountNumber] = acc
        addToLedger(CreateAccountAction(acc.accountNumber,initialAmount))
        return acc.accountNumber
    }

    fun getAccount(accountNumber: String): Account? {
        return accountsMap[accountNumber]
    }

    operator fun get(accountNumber: String): Account? {
        return accountsMap[accountNumber]
    }

    fun getAccountAvailable(): Long = maxAccount - accountsMap.keys().toList().size

    fun getAccounts(): List<Account> = accountsMap.values.toList().sortedBy { it.accountNumber }

    fun addToLedger(accountAction : AccountAction){
        STM.atomic(Callable {
            ledger.transform{
                it + accountAction
            }
        })
    }
}