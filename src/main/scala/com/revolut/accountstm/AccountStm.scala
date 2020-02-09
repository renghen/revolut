package com.revolut.accountstm

import zio._
import zio.console._
import zio.stm._

object IntegrationExample {
  val runtime = new DefaultRuntime {}

  runtime.unsafeRun(putStrLn("Hello World!"))

  val createTRef: STM[Nothing, TRef[Int]] = TRef.make(10)
}
