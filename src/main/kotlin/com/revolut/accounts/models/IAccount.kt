package com.revolut.accounts.models

import com.revolut.accounts.models.Account
import com.revolut.accounts.models.NotEnoughMoneyException

interface IAccount {
    @Throws(IllegalArgumentException::class)
    fun addMoney(amount: Double): Double

    @Throws(IllegalArgumentException::class)
    operator fun plus(amount: Double): Double

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    fun removeMoney(amount: Double): Double

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    operator fun minus(amount: Double): Double

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    fun transferTo(other: Account, amount: Double)

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class,
            TransferFeeDoesNotExistException::class, AccountNotFoundException::class)
    fun transferToAccountInOtherBank(otherBank: String, otherAccountName: String, amount: Double)

    fun balance(): Double
}