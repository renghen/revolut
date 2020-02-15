package com.revolut.accounts.models

import com.revolut.accounts.utils.BankUtils
import scala.concurrent.stm.*
import scala.concurrent.stm.japi.STM
import java.time.LocalDateTime
import java.util.concurrent.Callable

class NotEnoughMoneyException : Exception("Not enough money")
class TransferFeeDoesNotExistException : Exception("Transfer fee does not exist")
class AccountNotFoundException : Exception("Account number not found")

data class AccountDetails(val fullName: String)

sealed class AccountAction
data class CreateAccountAction(val accountNumber: String, val initialBalance: Double, val dateTime: LocalDateTime = LocalDateTime.now()) : AccountAction()
data class AddMoneyAction(val accountNumber: String, val amount: Double, val dateTime: LocalDateTime = LocalDateTime.now()) : AccountAction()
data class RemoveMoneyAction(val accountNumber: String, val amount: Double, val dateTime: LocalDateTime = LocalDateTime.now()) : AccountAction()

class Account private constructor(val accountNumber: String, val accountDetails: AccountDetails,
                                  initialBalance: Double, val bank: Bank) : IAccount {

    companion object {
        @Throws(AccountFullException::class)
        fun createAccount(accountDetails: AccountDetails, amount: Double, bank: Bank): Account {
            return Account(bank.generateAccountNumber(), accountDetails, amount, bank)
        }
    }

    private val balance: Ref.View<Double> = STM.newRef<Double>(initialBalance)

    @Throws(IllegalArgumentException::class)
    override fun addMoney(amount: Double): Double {
        if (amount < 0) {
            throw IllegalArgumentException("Wrong amount")
        }

        return STM.atomic(Callable {
            balance.transform {
                it + amount
            }
            this.bank.ledger.transform {
                it + AddMoneyAction(accountNumber,amount)
            }
            balance.get()
        })
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
            this.bank.ledger.transform {
                it + RemoveMoneyAction(accountNumber,amount)
            }
            balance.get()
        })
    }

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    override operator fun minus(amount: Double): Double {
        return removeMoney(amount)
    }

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class)
    override fun transferTo(other: Account, amount: Double) {
        STM.atomic {
            removeMoney(amount)
            other.addMoney(amount)
        }
    }

    @Throws(IllegalArgumentException::class, NotEnoughMoneyException::class,
            TransferFeeDoesNotExistException::class, AccountNotFoundException::class)
    override fun transferToAccountInOtherBank(otherBank: String, otherAccountName: String, amount: Double) {
        STM.atomic {
            val transferFee = bank.getForeignBankFee(otherBank)
            if (transferFee == null) {
                throw TransferFeeDoesNotExistException()
            } else {
                val otherAccount = bank.getForeignAccount(otherBank, otherAccountName)
                if (otherAccount == null) {
                    throw AccountNotFoundException()
                } else {
                    val transferFeeAmount = BankUtils.calculateInterBankFee(transferFee, amount)
                    removeMoney(amount + transferFeeAmount)
                    otherAccount.addMoney(amount)
                }
            }
        }
    }


    override fun balance(): Double = balance.get()
}