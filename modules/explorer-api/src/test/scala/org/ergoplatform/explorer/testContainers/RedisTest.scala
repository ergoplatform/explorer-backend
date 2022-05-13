package org.ergoplatform.explorer.testContainers

import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.scalatest.TestSuite
import org.testcontainers.containers.wait.strategy.Wait

trait RedisTest extends ForAllTestContainer {
  self: TestSuite =>

  override val container: GenericContainer =
    GenericContainer("redis:5.0.3", exposedPorts = Seq(6379), waitStrategy = Wait.forListeningPort())

}
