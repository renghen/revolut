package com.revolut.accounts.models

interface IAccount {
    @Throws(IllegalArgumentException::class)
    fun addMoney(amount: Double): Double

    @Throws(IllegalArgumentException::class)
    operator fun plus(amount: Double): Double

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    fun removeMoney(amount: Double): Double

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    operator fun minus(amount: Double): Double

    fun transferTo(other: Account, amount: Double)
}