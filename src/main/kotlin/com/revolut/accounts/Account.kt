package com.revolut.accounts

import org.multiverse.api.StmUtils
import org.multiverse.api.references.TxnDouble
import org.multiverse.api.references.TxnLong

class NotEnoughMoneyException : Exception("Not enough money")


class Account private constructor (accountNumber: String, initialBalance: Double) {

    companion object{
          fun createAccount(amount:Double) : Account{
              return Account("",amount)
          }
    }

    private val accountNumber = accountNumber
    private val lastUpdate: TxnLong = StmUtils.newTxnLong(System.currentTimeMillis())
    private val balance: TxnDouble = StmUtils.newTxnDouble(initialBalance)

    @Throws(IllegalArgumentException::class)
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

    @Throws(IllegalArgumentException::class)
    operator fun plus(amount: Double): Double {
        return addMoney(amount)
    }

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
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

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    operator fun minus(amount: Double): Double {
        return removeMoney(amount)
    }

    fun transferTo(other: Account, amount: Double) {
        StmUtils.atomic(Runnable {
            val date = System.currentTimeMillis()
            removeMoney(amount)
            other.addMoney(amount)
        })
    }


}