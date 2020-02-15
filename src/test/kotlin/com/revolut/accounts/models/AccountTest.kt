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
        val beforeTransactions = account.bank.ledger.get().count()

        assertFailsWith(IllegalArgumentException::class) {
            account.addMoney(-1.0)
        }
        assertEquals(beforeTransactions, account.bank.ledger.get().size)
    }

    @Test
    fun `remove incorrect amount`() {
        val account = bank["0005"]!!
        val beforeTransactions = account.bank.ledger.get().count()

        assertFailsWith(IllegalArgumentException::class) {
            account.removeMoney(-1.0)
        }
        assertEquals(beforeTransactions, account.bank.ledger.get().size)
    }

    @Test
    fun `remove too much`() {
        val account = bank["0005"]!!
        val beforeTransactions = account.bank.ledger.get().count()

        assertFailsWith(NotEnoughMoneyException::class) {
            try {
                account.removeMoney(account.balance() + 10)
            } catch (e: Exception) {
                throw e.cause ?: e
            }
        }
        assertEquals(beforeTransactions, account.bank.ledger.get().size)
    }

    @Test
    fun `increment account by 1 for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val account = bank["0000"]!!
        val beforeTransactions = account.bank.ledger.get().count()
        val futures = (1..1000).map {
            CompletableFuture.supplyAsync(Supplier {
                account.addMoney(1.toDouble())
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(1100.00, account.balance())
        assertEquals(beforeTransactions + 1000, account.bank.ledger.get().size)
    }

    @Test
    fun `increment account by 1 and remove by 1 for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val account = bank["0002"]!!
        account.addMoney(1000.toDouble()) // to prevent under flow exception
        val beforeTransactions = account.bank.ledger.get().count()
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
        assertEquals(beforeTransactions + 1000, account.bank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 acc but with insufficient funds`() {
        val accountA = bank["0010"]!!
        val accountB = bank["0011"]!!
        val balanceA = accountA.balance()
        val beforeTransactions = accountA.bank.ledger.get().count()

        assertFailsWith(NotEnoughMoneyException::class) {
            accountA.transferTo(accountB, balanceA + 1)
        }
        assertEquals(beforeTransactions, accountA.bank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 acc but with wrong amount`() {
        val accountA = bank["0010"]!!
        val accountB = bank["0011"]!!
        val beforeTransactions = accountA.bank.ledger.get().count()

        assertFailsWith(IllegalArgumentException::class) {
            accountA.transferTo(accountB, -10.00)
        }
        assertEquals(beforeTransactions, accountA.bank.ledger.get().size)
    }

    @Test
    fun `transfers account for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0010"]!!
        val accountB = bank["0011"]!!

        val balanceA = accountA.balance()
        val balanceB = accountB.balance()
        val beforeTransactions = accountA.bank.ledger.get().count()
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
        assertEquals(beforeTransactions + 2000, accountA.bank.ledger.get().size)
    }


    @Test
    fun `transfers account between 2 accounts for 6000 times concurrently with add and remove added into the mix`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0010"]!!
        val accountB = bank["0011"]!!

        val balanceA = accountA.balance()
        val balanceB = accountB.balance()
        val beforeTransactions = accountA.bank.ledger.get().count()
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
        assertEquals(beforeTransactions + 4000 + 4000, accountA.bank.ledger.get().size)
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
        val beforeTransactions = accountA.bank.ledger.get().count()
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
        assertEquals(beforeTransactions + 4000 * 2, accountA.bank.ledger.get().size)
    }

    private val otherBank = banks["XYZ"]!!
    private val otherBankAccountNumber = "0050"

    @Test
    fun `transfers account between 2 different bank acc but with wrong bank fee`() {
        val accountA = bank["0050"]!!
        val bankDoesNotExist = "notExist"
        val beforeTransactions = accountA.bank.ledger.get().count()
        assertFailsWith(TransferFeeDoesNotExistException::class) {
            accountA.transferToAccountInOtherBank(bankDoesNotExist, otherBankAccountNumber, 10.0)
        }
        assertEquals(beforeTransactions, accountA.bank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 different bank acc but with wrong bank account`() {
        val accountA = bank["0050"]!!
        val accountB = "9000"
        val beforeTransactions = accountA.bank.ledger.get().count()

        assertFailsWith(AccountNotFoundException::class) {
            accountA.transferToAccountInOtherBank(otherBank.name, accountB, 10.0)
        }
        assertEquals(beforeTransactions, accountA.bank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 different bank acc but with wrong amount`() {
        val accountA = bank["0050"]!!
        val accountB = "0050"
        val beforeTransactions = accountA.bank.ledger.get().count()

        assertFailsWith(IllegalArgumentException::class) {
            accountA.transferToAccountInOtherBank(otherBank.name, accountB, -10.0)
        }
        assertEquals(beforeTransactions, accountA.bank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 different bank acc but with insufficient funds`() {
        val accountA = bank["0050"]!!
        val accountB = "0050"
        val beforeTransactions = accountA.bank.ledger.get().count()

        assertFailsWith(NotEnoughMoneyException::class) {
            accountA.transferToAccountInOtherBank(otherBank.name, accountB, 1000.0)
        }
        assertEquals(beforeTransactions, accountA.bank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 different bank acc with fix fee of 1`() {
        val accountA = bank["0050"]!!
        val accountB = "0050"
        val balanceA = accountA.balance()
        val amountToTransfer = 9.0
        val fee = BankUtils.calculateInterBankFee(bank.getForeignBankFee(otherBank.name)!!, amountToTransfer)
        val beforeTransactionsA = bank.ledger.get().count()
        val beforeTransactionsB = otherBank.ledger.get().count()

        accountA.transferToAccountInOtherBank(otherBank.name, accountB, amountToTransfer)
        assertEquals(balanceA - (amountToTransfer + fee), accountA.balance())
        assertEquals(beforeTransactionsA + 1, bank.ledger.get().size)
        assertEquals(beforeTransactionsB + 1, otherBank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 different bank acc with percentage of 5 per cent`() {
        val accountA = otherBank["0050"]!!
        val accountB = "0050"
        val balanceA = accountA.balance()
        val amountToTransfer = 10.0
        val beforeTransactionsA = bank.ledger.get().count()
        val beforeTransactionsB = otherBank.ledger.get().count()
        val fee = BankUtils.calculateInterBankFee(otherBank.getForeignBankFee(bank.name)!!, amountToTransfer)

        accountA.transferToAccountInOtherBank(bank.name, accountB, amountToTransfer)
        assertEquals(balanceA - (amountToTransfer + fee), accountA.balance())
        assertEquals(beforeTransactionsA + 1, bank.ledger.get().size)
        assertEquals(beforeTransactionsB + 1, otherBank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 different bank acc for 2000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0051"]!!
        val accountB = otherBank["0051"]!!

        val balanceA = accountA + 1000.0
        val balanceB = accountB + 1000.0
        val beforeTransactionsA = bank.ledger.get().count()
        val beforeTransactionsB = otherBank.ledger.get().count()
        val futures = (1..2000).map {
            CompletableFuture.supplyAsync(Supplier {
                if (it % 2 == 0) {
                    accountA.transferToAccountInOtherBank(otherBank.name, accountB.accountNumber, 10.toDouble())
                } else {
                    accountB.transferToAccountInOtherBank(bank.name, accountA.accountNumber, 10.toDouble())
                }
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(balanceA - 1000, accountA.balance())
        assertEquals(balanceB - 500, accountB.balance())
        assertEquals(beforeTransactionsA + 1000 + 1000, bank.ledger.get().size)
        assertEquals(beforeTransactionsB + 1000 + 1000, otherBank.ledger.get().size)
    }

    @Test
    fun `transfers account between 2 different bank accounts for 4000 times concurrently with add and remove added into the mix`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0052"]!!
        val accountB = otherBank["0052"]!!

        val balanceA = accountA.balance()
        val balanceB = accountB.balance()
        val beforeTransactionsA = bank.ledger.get().count()
        val beforeTransactionsB = otherBank.ledger.get().count()
        val futures = (1..4000).map {
            CompletableFuture.supplyAsync(Supplier {
                when (it % 4) {
                    0 -> accountA.transferToAccountInOtherBank(otherBank.name, accountB.accountNumber, 10.toDouble())
                    1 -> accountB.transferToAccountInOtherBank(bank.name, accountA.accountNumber, 10.toDouble())
                    2 -> {
                        accountA.addMoney(1.toDouble());Unit
                    }
                    3 -> {
                        accountB.addMoney(0.5);Unit
                    }
                }
            }, executorService)
        }.toTypedArray()

        CompletableFuture.allOf(*futures).get()
        assertEquals(balanceA, accountA.balance())
        assertEquals(balanceB, accountB.balance())
        assertEquals(beforeTransactionsA + 1000 + 1000 + 1000, bank.ledger.get().size)
        assertEquals(beforeTransactionsB + 1000 + 1000 + 1000, otherBank.ledger.get().size)
    }

}