package com.revolut.accounts.models

import org.multiverse.api.StmUtils
import org.multiverse.api.references.TxnDouble
import org.multiverse.api.references.TxnLong
import java.net.Inet4Address

class NotEnoughMoneyException : Exception("Not enough money")

data class AccountDetails(val fullName : String)

class Account private constructor(val accountNumber: String, val accountDetails: AccountDetails,
                                  initialBalance: Double, val bank: Bank) : IAccount {

    companion object {
        @Throws(AccountFullException::class)
        fun createAccount(accountDetails: AccountDetails, amount: Double, bank: Bank): Account {
            return Account(bank.generateAccountNumber(), accountDetails, amount, bank)
        }
    }

    private val lastUpdate: TxnLong = StmUtils.newTxnLong(System.currentTimeMillis())
    private val balance: TxnDouble = StmUtils.newTxnDouble(initialBalance)

    @Throws(IllegalArgumentException::class)
    override fun addMoney(amount: Double): Double {
        if (amount < 0) {
            throw IllegalArgumentException("Wrong amount")
        }

        StmUtils.atomic(Runnable {
            balance.atomicIncrementAndGet(amount)
            lastUpdate.set(System.currentTimeMillis())
        })
        return balance.atomicGet()
    }

    @Throws(IllegalArgumentException::class)
    override operator fun plus(amount: Double): Double {
        return addMoney(amount)
    }

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    override fun removeMoney(amount: Double): Double {
        if (amount < 0) {
            throw IllegalArgumentException("Wrong amount")
        }
        StmUtils.atomic(Runnable {
            val currentBalance = balance.atomicGet()
            if (currentBalance < amount) {
                throw NotEnoughMoneyException()
            }
            balance.atomicIncrementAndGet(-1 * amount)
            lastUpdate.set(System.currentTimeMillis())
        })
        return balance.atomicGet()
    }

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    override operator fun minus(amount: Double): Double {
        return removeMoney(amount)
    }

    override fun transferTo(other: Account, amount: Double) {
        StmUtils.atomic(Runnable {
            removeMoney(amount)
            other.addMoney(amount)
        })
    }

    fun balance() : Double = balance.atomicGet()
}