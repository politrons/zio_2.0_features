package com.politrons.zio

import org.junit.Test
import zio.{Layer, Runtime, Scope, ULayer, Unsafe, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}


class ZIONewFeatures {

  val runtime = Runtime.default

  /**
   * Now with ZIO 2.o0 the use of layers in the program as dependencies is far easier and less verbose
   * than the use of Has[]
   * Now we just need to define the dependency in the [R,_,_] type, and use inside the program,
   * using ZIO.service[String] inside a for comprehension
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
      val value = runtime.unsafe.run(programWithDependencies).getOrThrowFiberFailure()
      println(s"Result $value")
    }
  }


}