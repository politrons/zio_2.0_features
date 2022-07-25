package com.politrons.zio

import org.junit.Test
import zio.Runtime.default
import zio.{Layer, Runtime, Schedule, Scope, UIO, ULayer, Unsafe, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, ZState}

import java.util.Random


class ZIONewFeatures {


  val runtime: default.UnsafeAPI = Runtime.default.unsafe

  /**
   * Now with ZIO 2.o0 the use of layers in the program as dependencies is far easier and less verbose
   * than the use of Has[]
   * Now we just need to define the dependency in the [R,_,_] type, and use inside the program,
   * using ZIO.service[String] inside a for comprehension
   *
   * To be passed into the program we need to use the operator [provideLayer] in the program, and pass
   * the value of the dependency.
   */
  @Test
  def layerFeatures(): Unit = {

    val zioProgram: ZIO[String, Throwable, String] = for {
      value <- ZIO.service[String]
      newValue <- ZIO.succeed(value.toUpperCase())
    } yield newValue

    val dependency: ULayer[String] = ZLayer.succeed("hello zio world 2.0")

    Unsafe.unsafe { implicit unsafe =>
      val programWithDependencies = zioProgram.provideLayer(dependency)
      val value = runtime.run(programWithDependencies).getOrThrowFiberFailure()
      println(s"Result $value")
    }
  }

  /**
   * Make a program run with multiple dependencies, is now so simple like mark the dependency type
   * with [with] without have to use [has] as we did with ZIO 1.0
   *
   * Then we need to create a ZLayer with this two dependencies in the type, just adding both with [++]
   */
  @Test
  def multipleLayerFeatures(): Unit = {

    val zioProgram: ZIO[String with Int, Throwable, String] = for {
      strValue <- ZIO.service[String]
      intValue <- ZIO.service[Int]
      newValue <- ZIO.succeed(s"${strValue.toUpperCase()} - ${intValue + 10}")
    } yield newValue

    val strDependency = ZLayer.succeed("hello zio world 2.0")
    val intDependency = ZLayer.succeed(1981)

    val dependency: ULayer[String with Int] = strDependency ++ intDependency

    Unsafe.unsafe { implicit unsafe =>

      val programWithDependencies = zioProgram.provideLayer(dependency)
      val value = runtime.run(programWithDependencies).getOrThrowFiberFailure()
      println(s"Result $value")
    }
  }

  /**
   * In ZIO 2.0 effect operator was used to create an ZIO program with an effect, with a possible side-effect
   * Now with version 2.0 we change that operator by [attempt] which it return an effect system with
   * type [Throwable] as possible side-effect type
   */
  @Test
  def attemptFeatures(): Unit = {

    val program: ZIO[Any, Throwable, String] = ZIO.attempt("hello unsafe code")
      .map(value => value.toUpperCase)

    Unsafe.unsafe { implicit unsafe =>
      val value = runtime.run(program).getOrThrowFiberFailure()
      println(s"Result $value")
    }
  }


  /**
   * In version 2.0 is easier than ever to run an effect in another thread(Fiber)
   * We just need to use [async] operator which it will provide a [callback] function that need to be filled
   * with the ZIO.program to be run in the Fiber Thread.
   */
  @Test
  def asyncFeatures(): Unit = {

    val asyncProgram = ZIO.async[Any, Throwable, String] { callback =>

      val program = for {
        value <- ZIO.succeed(s"hello Async world in Thread ${Thread.currentThread().getName}")
        newValue <- ZIO.attempt(value.toUpperCase)
      } yield newValue

      callback(program)
    }

    Unsafe.unsafe { implicit unsafe =>
      val value = runtime.run(asyncProgram).getOrThrowFiberFailure()
      println(s"Result $value")
    }
  }

  @Test
  def zStateFeatures(): Unit = {

    val statefulProgram: ZIO[ZState[String], Nothing, String] = for {
      state <- ZIO.service[ZState[String]]
      _ <- state.update(value => value.toUpperCase + "!!!!")
      value <- state.get
    } yield value

    Unsafe.unsafe { implicit unsafe =>
      val program = ZIO.stateful("Stateful value to be used in program")(statefulProgram)
      val value = runtime.run(program).getOrThrowFiberFailure()
      println(s"Result: $value")
    }
  }

}
