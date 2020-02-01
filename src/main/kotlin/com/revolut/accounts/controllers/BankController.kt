package com.revolut.accounts.controllers

import com.revolut.accounts.models.Bank
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.routing.path

fun bankApp(bank: Bank): List<RoutingHttpHandler> =
        listOf(
                "/" bind GET to { Response(OK) },
                "/bank" bind GET to {
                    Response(OK).body("{bank : ${bank.name}}")
                },
                "/bank/accountsLeft" bind GET to {
                    Response(OK).body("{accountsLeft : ${bank.getAccountAvailable()}}")
                },
                "/bank/accounts" bind GET to {
                    //TODO json encoding
                    Response(OK).body("{bank : ${bank.name}}")
                },
                "/bank/accounts/{accountNumber}" bind GET to { req : Request ->

                    val accountNumber = req.path("accountNumber")

                    if(accountNumber == null){
                        Response(BAD_REQUEST).body("""{message : "bad request"}""")
                    }else{
                        val account = bank[accountNumber]
                        if (account == null) {
                            Response(NOT_FOUND).body("""{message : "account number not found"}""")
                        }
                        else{
                            //TODO json encoding
                            Response(OK).body("{bank : ${bank.name}}")
                        }
                    }
                }
        )
