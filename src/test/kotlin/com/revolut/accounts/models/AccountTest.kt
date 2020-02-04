package com.revolut.accounts.models

import com.revolut.accounts.utils.BankUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.test.*

class AccountTest {
    private val banks = ConcurrentHashMap<String, Bank>()

    init {
        val bankA = BankUtils.`concurrent creation of N Accounts`(100, name = "ABC")
        val bankB = BankUtils.`concurrent creation of N Accounts`(100, name = "XYZ")
        bankA.addForeignBank(bankB, FixedFee(1.0))
        bankB.addForeignBank(bankA, PercentageFee(5.0))
        banks[bankA.name] = bankA
        banks[bankB.name] = bankB
    }

    private val bank = banks["ABC"]!!

    @Test
    fun `add incorrect amount`() {
        val account = bank["0005"]!!

        assertFailsWith(IllegalArgumentException::class) {
            account.addMoney(-1.0)
        }
    }

    @Test
    fun `remove incorrect amount`() {
        val account = bank["0005"]!!

        assertFailsWith(IllegalArgumentException::class) {
            account.removeMoney(-1.0)
        }
    }

    @Test
    fun `remove too much`() {
        val account = bank["0005"]!!

        assertFailsWith(NotEnoughMoneyException::class) {
            try {
                account.removeMoney(account.balance() + 10)
            } catch (e: Exception) {
                throw e.cause ?: e
            }
        }
    }

    @Test
    fun `increment account by 1 for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val account = bank["0000"]!!
        val futures = (1..1000).map {
            CompletableFuture.supplyAsync(Supplier {
                account.addMoney(1.toDouble())
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(1100.00, account.balance())
    }

    @Test
    fun `increment account by 1 and remove by 1 for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val account = bank["0002"]!!
        account.addMoney(1000.toDouble()) // to prevent under flow exception
        val futures = (1..1000).map {
            CompletableFuture.supplyAsync(Supplier {
                if (it % 2 == 0) {
                    account.addMoney(1.toDouble())
                } else {
                    account.removeMoney(1.toDouble())
                }
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(1100.00, account.balance())
    }

    @Test
    fun `transfers account between 2 acc but with insufficient funds`() {
        val accountA = bank["0010"]!!
        val accountB = bank["0011"]!!
        val balanceA = accountA.balance()

        assertFailsWith(NotEnoughMoneyException::class) {
            accountA.transferTo(accountB, balanceA + 1)
        }
    }

    @Test
    fun `transfers account between 2 acc but with wrong amount`() {
        val accountA = bank["0010"]!!
        val accountB = bank["0011"]!!

        assertFailsWith(IllegalArgumentException::class) {
            accountA.transferTo(accountB, -10.00)
        }
    }

    @Test
    fun `transfers account for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0010"]!!
        val accountB = bank["0011"]!!

        val balanceA = accountA.balance()
        val balanceB = accountB.balance()
        val futures = (1..1000).map {
            CompletableFuture.supplyAsync(Supplier {
                if (it % 2 == 0) {
                    accountA.transferTo(accountB, 1.toDouble())
                } else {
                    accountB.transferTo(accountA, 1.toDouble())
                }
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(balanceA, accountA.balance())
        assertEquals(balanceB, accountB.balance())
    }


    @Test
    fun `transfers account between 2 accounts for 6000 times concurrently with add and remove added into the mix`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0010"]!!
        val accountB = bank["0011"]!!

        val balanceA = accountA.balance()
        val balanceB = accountB.balance()
        val futures = (1..6000).map {
            CompletableFuture.supplyAsync(Supplier {
                when (it % 6) {
                    0 -> accountA.transferTo(accountB, 1.toDouble())
                    1 -> accountB.transferTo(accountA, 1.toDouble())
                    2 -> {
                        accountA.addMoney(1.toDouble());Unit
                    }
                    3 -> {
                        accountB.addMoney(1.toDouble());Unit
                    }
                    4 -> {
                        accountA.removeMoney(1.toDouble());Unit
                    }
                    5 -> {
                        accountB.removeMoney(1.toDouble());Unit
                    }
                }
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(balanceA, accountA.balance())
        assertEquals(balanceB, accountB.balance())
    }

    @Test
    fun `transfers account between 3 accounts for 4000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0012"]!!
        val accountB = bank["0013"]!!
        val accountC = bank["0014"]!!
        val balanceA = accountA.balance()
        val balanceB = accountB.balance()
        val balanceC = accountC.balance()
        accountC.addMoney(2000.00)
        val futures = (1..4000).map {
            CompletableFuture.supplyAsync(Supplier {
                when (it % 4) {
                    0 -> accountA.transferTo(accountB, 1.toDouble())
                    1 -> accountB.transferTo(accountA, 1.toDouble())
                    2 -> accountC.transferTo(accountA, 1.toDouble())
                    3 -> accountC.transferTo(accountB, 1.toDouble())
                }
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(balanceA + 1000.00, accountA.balance())
        assertEquals(balanceB + 1000.00, accountB.balance())
        assertEquals(balanceC, accountC.balance())
    }

    private val otherBank = banks["XYZ"]!!
    private val otherBankAccountNumber = "0050"

    @Test
    fun `transfers account between 2 different bank acc but with wrong bank fee`() {
        val accountA = bank["0050"]!!
        val bankDoesNotExist = "notExist"

        assertFailsWith(TransferFeeDoesNotExistException::class) {
            accountA.transferToAccountInOtherBank(bankDoesNotExist, otherBankAccountNumber, 10.0)
        }
    }

    @Test
    fun `transfers account between 2 different bank acc but with wrong bank account`() {
        val accountA = bank["0050"]!!
        val accountB = "9000"

        assertFailsWith(AccountNotFoundException::class) {
            accountA.transferToAccountInOtherBank(otherBank.name, accountB, 10.0)
        }
    }

    @Test
    fun `transfers account between 2 different bank acc but with wrong amount`() {
        val accountA = bank["0050"]!!
        val accountB = "0050"

        assertFailsWith(IllegalArgumentException::class) {
            accountA.transferToAccountInOtherBank(otherBank.name, accountB, -10.0)
        }
    }

    @Test
    fun `transfers account between 2 different bank acc but with insufficient funds`() {
        val accountA = bank["0050"]!!
        val accountB = "0050"

        assertFailsWith(NotEnoughMoneyException::class) {
            accountA.transferToAccountInOtherBank(otherBank.name, accountB, 1000.0)
        }
    }

}