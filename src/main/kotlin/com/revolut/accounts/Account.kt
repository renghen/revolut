package com.revolut.accounts

import org.multiverse.api.StmUtils
import org.multiverse.api.references.TxnDouble
import org.multiverse.api.references.TxnLong

class NotEnoughMoneyException : Exception("Not enough money")


class Account(accountNumber: String, initialBalance: Double) {
    private val accountNumber = accountNumber
    private val lastUpdate: TxnLong = StmUtils.newTxnLong(System.currentTimeMillis())
    private val balance: TxnDouble = StmUtils.newTxnDouble(initialBalance)

    fun addMoney(amount: Double): Double {
        if (amount < 0) {
            throw IllegalArgumentException("Wrong amount");
        }

        StmUtils.atomic(Runnable {
            balance.atomicIncrementAndGet(amount)
            lastUpdate.set(System.currentTimeMillis())
        })
        return balance.atomicGet()
    }

    fun removeMoney(amount: Double): Double {
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

    fun transferTo(other: Account, amount: Double) {
        StmUtils.atomic(Runnable {
            val date = System.currentTimeMillis()
            removeMoney(amount)
            other.addMoney(amount)
        })
    }


}