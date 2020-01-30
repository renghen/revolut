package com.revolut.accounts

import org.multiverse.api.StmUtils
import org.multiverse.api.references.TxnLong


class Bank {

    companion object {
        private val maxAccount: Long = 1000
        val currentAccount: TxnLong = StmUtils.newTxnLong(0)

        //utility to generate number
        fun generateAccountNumber(): String =
                currentAccount.atomicIncrementAndGet(1).toString().padStart(4, '0')

    }

}