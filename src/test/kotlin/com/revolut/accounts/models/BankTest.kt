package com.revolut.accounts.models

import com.revolut.accounts.models.Bank.Companion.NoFee
import com.revolut.accounts.models.Bank.Companion.formatIntForAcountNumber
import java.lang.Exception
import com.revolut.accounts.utils.BankUtils.`concurrent creation of N Accounts`
import kotlin.test.*

class BankTest {

    @Test
    fun `bank creation`() {
        val bank = Bank("abc")
        assertEquals(bank.getAccountAvailable(), 1000)
    }

    @Test
    fun `100 account creation`() {
        val bank = Bank("abc")
        for (i in 1..100) {
            val accountDetails = AccountDetails(i.toString())
            val accountNumber = bank.createAccount(accountDetails, 100.00)
            assertEquals(formatIntForAcountNumber(i.toLong() - 1), accountNumber)
        }
        assertEquals(900, bank.getAccountAvailable())
    }

    @Test
    fun `100 account creation concurrent`() {
        val bank = `concurrent creation of N Accounts`(100)
        assertEquals(900, bank.getAccountAvailable())
    }

    @Test
    fun `100 account creation concurrent with same initial amount`() {
        val bank = `concurrent creation of N Accounts`(100)
        val accounts = bank.getAccounts()
        var i = 0
        accounts.forEach {
            val expectedAccountNumber = formatIntForAcountNumber(i.toLong())
            i += 1
            assertEquals(expectedAccountNumber, it.accountNumber)
            assertEquals(100.00, it.balance())
        }
    }

    @Test
    fun `1001 account creation concurrent should produce an exception`() {
        assertFailsWith(AccountFullException::class) {
            try {
                `concurrent creation of N Accounts`(1001)
            } catch (e: Exception) {
                println(e.cause)
                throw e.cause ?: e
            }
        }
    }

    @Test
    fun `1001 account creation concurrent should still have 1000 accounts,even with exception`() {
        val bank: Bank = `concurrent creation of N Accounts`(1000)
        try {
            bank.createAccount(AccountDetails("1001"), 100.0)
        } catch (e: Exception) {
            println(e.cause?.cause)
        }
        assertEquals(1000, bank.getAccounts().size)
    }

    @Test
    fun `interbank test`() {
        val bankA = Bank("abc")
        val bankB = Bank("xyz")
        val bankC = Bank("def")

        bankA.addForeignBank(bankB, NoFee)
        bankA.addForeignBank(bankC, NoFee)

        bankB.addForeignBank(bankA, PercentageFee(5.0))
        bankB.addForeignBank(bankC, NoFee)

        bankC.addForeignBank(bankA, FixedFee(1.0))
        bankC.addForeignBank(bankB, NoFee)

        assertEquals(NoFee, bankA.getForeignBankFee(bankB.name))
        assertEquals(NoFee, bankA.getForeignBankFee(bankC.name))
        assertNull(bankA.getForeignBankFee("notExist"))

        assertEquals(PercentageFee(5.0), bankB.getForeignBankFee(bankA.name))
        assertEquals(NoFee, bankB.getForeignBankFee(bankC.name))
        assertNull(bankB.getForeignBankFee("notExist"))

        assertEquals(FixedFee(1.0), bankC.getForeignBankFee(bankA.name))
        assertEquals(NoFee, bankC.getForeignBankFee(bankB.name))
        assertNull(bankC.getForeignBankFee("notExist"))
    }
}


