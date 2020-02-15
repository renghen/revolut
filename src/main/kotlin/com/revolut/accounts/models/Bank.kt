package com.revolut.accounts.models

import scala.concurrent.stm.japi.STM
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

class AccountFullException : Exception("Account Full")

sealed class InterBankFee
data class PercentageFee(val amount: Double) : InterBankFee()
data class FixedFee(val amount: Double) : InterBankFee()

data class OtherBankDetails(val bank : Bank, val fees : InterBankFee)

class Bank constructor(val name: String) {

    companion object{
        fun formatIntForAcountNumber(l : Long) : String = l.toString().padStart(4, '0')
        val NoFee = FixedFee(0.0)
    }

    private val maxAccount: Long = 1000
    private val accountsMap: ConcurrentHashMap<String, Account> = ConcurrentHashMap()
    private var currentAccount: Long = -1
    private val otherBanksFeeMap: ConcurrentHashMap<String, OtherBankDetails> = ConcurrentHashMap()
    val ledger = STM.newRef<List<AccountAction>>(listOf())

    init {
        otherBanksFeeMap[this.name]= OtherBankDetails(this,NoFee)
    }


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
        STM.atomic(Callable {
            ledger.transform{
                it + CreateAccountAction(acc.accountNumber,initialAmount)
            }
        })
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

    @Synchronized
    fun addForeignBank(bank: Bank, fee: InterBankFee){
        otherBanksFeeMap.putIfAbsent(bank.name,OtherBankDetails(bank,fee))
    }

    fun getForeignBankFee(bankName: String) : InterBankFee? {
        return otherBanksFeeMap[bankName]?.fees
    }

    fun getForeignAccount(otherBankName: String, accountNumber: String) : Account?{
        return otherBanksFeeMap[otherBankName]?.bank?.getAccount(accountNumber)
    }

}