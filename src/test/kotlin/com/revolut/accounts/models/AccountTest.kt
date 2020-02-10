package com.revolut.accounts.models

import com.revolut.accounts.utils.BankUtils
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Supplier
import kotlin.test.*

class AccountTest {

    companion object {
        val bank = BankUtils.`concurrent creation of N Accounts`(100)
    }

    @Test
    fun `add incorrect amount`() {
        val account = bank["0005"]
        if (account != null) {
            assertFailsWith(IllegalArgumentException::class) {
                account.addMoney(-1.0)
            }
        } else {
            assertNotNull<Account>(account, "account '0005' cannot be null")
        }
    }

    @Test
    fun `remove incorrect amount`() {
        val account = bank["0005"]
        if (account != null) {
            assertFailsWith(IllegalArgumentException::class) {
                account.removeMoney(-1.0)
            }
        } else {
            assertNotNull<Account>(account, "account '0005' cannot be null")
        }
    }

    @Test
    fun `remove too much`() {
        val account = bank["0005"]
        if (account != null) {
            assertFailsWith(NotEnoughMoneyException::class) {
                try {
                    account.removeMoney(account.balance() + 10)
                } catch (e: Exception) {
                    throw e.cause ?: e
                }
            }
        } else {
            assertNotNull<Account>(account, "account '0005' cannot be null")
        }
    }

    @Test
    fun `increment account by 1 for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val account = bank["0000"]

        if (account != null) {
            val futures = (1..1000).map {
                CompletableFuture.supplyAsync(Supplier {
                    account.addMoney(1.toDouble())
                }, executorService)
            }.toTypedArray()

            CompletableFuture.allOf(*futures).get()
            assertEquals(1100.00, account.balance())
        } else {
            assertNotNull<Account>(account, "account '0000' cannot be null")
        }
    }

    @Test
    fun `increment account by 1 and remove by 1 for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val account = bank["0002"]

        if (account != null) {
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
        } else {
            assertNotNull<Account>(account, "account '0002' cannot be null")
        }
    }

    @Test
    fun `transfers account between 2 acc but with unsufficient amount`() {
        val accountA = bank["0010"]
        val accountB = bank["0011"]

        if (accountA != null && accountB != null) {
            val balanceA = accountA.balance()
            assertFailsWith(NotEnoughMoneyException::class) {
                accountA.transferTo(accountB, balanceA + 1)
            }
        } else {
            assertNotNull<Account>(accountA, "account '0010' cannot be null")
            assertNotNull<Account>(accountB, "account '0011' cannot be null")
        }
    }

    @Test
    fun `transfers account between 2 acc but with wrong amount`() {
        val accountA = bank["0010"]
        val accountB = bank["0011"]

        if (accountA != null && accountB != null) {
            assertFailsWith(IllegalArgumentException::class) {
                accountA.transferTo(accountB, -10.00)
            }
        } else {
            assertNotNull<Account>(accountA, "account '0010' cannot be null")
            assertNotNull<Account>(accountB, "account '0011' cannot be null")
        }
    }

    @Test
    fun `transfers account for 1000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0010"]
        val accountB = bank["0011"]

        if (accountA != null && accountB != null) {
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
        } else {
            assertNotNull<Account>(accountA, "account '0010' cannot be null")
            assertNotNull<Account>(accountB, "account '0011' cannot be null")
        }
    }


    @Test
    fun `transfers account between 2 accounts for 6000 times concurrently with add and remove added into the mix`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0010"]
        val accountB = bank["0011"]

        if (accountA != null && accountB != null) {
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
        } else {
            assertNotNull<Account>(accountA, "account '0010' cannot be null")
            assertNotNull<Account>(accountB, "account '0011' cannot be null")
        }
    }

    @Test
    fun `transfers account between 3 accounts for 4000 times concurrently`() {
        val executorService = Executors.newFixedThreadPool(10)
        val accountA = bank["0012"]
        val accountB = bank["0013"]
        val accountC = bank["0014"]

        if (accountA != null && accountB != null && accountC != null) {
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
        } else {
            assertNotNull<Account>(accountA, "account '0012' cannot be null")
            assertNotNull<Account>(accountB, "account '0013' cannot be null")
            assertNotNull<Account>(accountC, "account '0014' cannot be null")
        }
    }

}