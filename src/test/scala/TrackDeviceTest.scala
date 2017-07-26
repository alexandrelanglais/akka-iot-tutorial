import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration.FiniteDuration

class TrackDeviceTest(_system: ActorSystem) extends TestKit(_system)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("iotAkka-test"))

  override def afterAll(): Unit = shutdown(system)

  "A device Actor" should "reply with empty reading if no temperature is known" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.ReadTemperature(requestId = 42), probe.ref)
    val response = probe.expectMsgType[Device.RespondTemperature]
    response.requestId should ===(42)
    response.value should ===(None)
  }

  it should "respond with a temperature recorded message when sent one" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.RecordTemperature(requestId = 42, value = 37.0), probe.ref)
    val response = probe.expectMsgType[Device.TemperatureRecorded]
    response.requestId should ===(42)
  }

  it should "reply with the latest temperature reading" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.RecordTemperature(requestId = 42, value = 37.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 42))

    deviceActor.tell(Device.ReadTemperature(requestId = 43), probe.ref)
    val response = probe.expectMsgType[Device.RespondTemperature]
    response.requestId should ===(43)
    response.value should ===(Some(37.0))

    deviceActor.tell(Device.RecordTemperature(requestId = 44, value = 39.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 44))

    deviceActor.tell(Device.ReadTemperature(requestId = 45), probe.ref)
    val response2 = probe.expectMsgType[Device.RespondTemperature]
    response2.requestId should ===(45)
    response2.value should ===(Some(39.0))
  }
  it should "reply to registration requests" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(RequestTrackDevice("group", "device"), probe.ref)
    probe.expectMsg(DeviceRegistered)
    probe.lastSender === (deviceActor)
  }
  it should "ignore invalid registration requests" in {
    val probe = TestProbe()
    val deviceActor = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(RequestTrackDevice("bla", "blabla"), probe.ref)
    probe.expectNoMsg(new FiniteDuration(500, TimeUnit.MILLISECONDS))
  }
}
