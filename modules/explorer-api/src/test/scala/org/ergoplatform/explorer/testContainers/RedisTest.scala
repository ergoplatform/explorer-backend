package org.ergoplatform.explorer.testContainers

import com.dimafeng.testcontainers.{ForAllTestContainer, GenericContainer}
import org.scalatest.TestSuite
import org.testcontainers.containers.wait.strategy.Wait

trait RedisTest extends ForAllTestContainer {
  self: TestSuite =>

  val redisTestPort = 6379

  override val container: GenericContainer =
    GenericContainer("redis:5.0.3", exposedPorts = Seq(redisTestPort), waitStrategy = Wait.forListeningPort())

}
