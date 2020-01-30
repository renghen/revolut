package com.revolut.accounts

import org.multiverse.api.StmUtils
import org.multiverse.api.references.TxnDouble
import org.multiverse.api.references.TxnLong

class NotEnoughMoneyException : Exception("Not enough money")


class Account private constructor (accountNumberParam: String, initialBalance: Double,bankParam :Bank) : IAccount {

    companion object{
          fun createAccount(amount:Double,bank :Bank) : Account{
              return Account(bank.generateAccountNumber(),amount,bank)
          }
    }

    val accountNumber = accountNumberParam
    private val lastUpdate: TxnLong = StmUtils.newTxnLong(System.currentTimeMillis())
    private val balance: TxnDouble = StmUtils.newTxnDouble(initialBalance)
    val bank : Bank = bankParam

    @Throws(IllegalArgumentException::class)
    override fun addMoney(amount: Double): Double {
        if (amount < 0) {

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
}