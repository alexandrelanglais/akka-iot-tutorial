import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration.FiniteDuration

class DeviceManagerTest(_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("iotAkka-test"))

  override def afterAll(): Unit = shutdown(system)

  "A device manager" should "be able to register a device group" in {
    val probe = TestProbe()
    val managerActor = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor1 = probe.lastSender

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)
    val deviceActor2 = probe.lastSender
    deviceActor1 should !==(deviceActor2)

    // Check that the device actors are working
    deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
    deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))
  }

  it should "be able to list active groups" in {
    val probe = TestProbe()
    val managerActor = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    managerActor.tell(DeviceManager.RequestTrackDevice("group2", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered)

    managerActor.tell(DeviceManager.RequestGroupList(requestId = 0), probe.ref)
    probe.expectMsg(DeviceManager.ReplyGroupList(requestId = 0, Set("group", "group2")))
  }

}
