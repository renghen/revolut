package com.revolut.accounts.models

import scala.concurrent.stm.*
import scala.concurrent.stm.japi.STM
import java.util.concurrent.Callable

class NotEnoughMoneyException : Exception("Not enough money")

data class AccountDetails(val fullName: String)

class Account private constructor(val accountNumber: String, val accountDetails: AccountDetails,
                                  initialBalance: Double, val bank: Bank) : IAccount {

    companion object {
        @Throws(AccountFullException::class)
        fun createAccount(accountDetails: AccountDetails, amount: Double, bank: Bank): Account {
            return Account(bank.generateAccountNumber(), accountDetails, amount, bank)
        }
    }

    private val balance: Ref.View<Double> = STM.newRef(initialBalance) as Ref.View<Double>

    @Throws(IllegalArgumentException::class)
    override fun addMoney(amount: Double): Double {
        if (amount < 0) {
            throw IllegalArgumentException("Wrong amount")
        }

        STM.atomic(Runnable {
            balance.transform {
                it + amount
            }
        })
        return balance.get()
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

        return STM.atomic(Callable {
            val currentBalance = balance.get()
            if (currentBalance < amount) {
                throw NotEnoughMoneyException()
            }
            balance.transform {
                it - amount
            }
            balance.get()
        })
    }

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    override operator fun minus(amount: Double): Double {
        return removeMoney(amount)
    }

    override fun transferTo(other: Account, amount: Double) {
        STM.atomic(Runnable {
            removeMoney(amount)
            other.addMoney(amount)
        })
    }

    override fun balance(): Double = balance.get()
}