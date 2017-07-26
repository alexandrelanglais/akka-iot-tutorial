import Device.{ReadTemperature, RecordTemperature, RespondTemperature, TemperatureRecorded}
import akka.actor.{Actor, ActorLogging, Props}

object Device {
  def props(groupId: String, deviceId: String): Props = Props(new Device(groupId, deviceId))

  final case class RecordTemperature(requestId: Long, value: Double)
  final case class TemperatureRecorded(requestId: Long)

  final case class ReadTemperature(requestId: Long)
  final case class RespondTemperature(requestId: Long, value: Option[Double])
}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {
  var lastTemperatureReading: Option[Double] = None

  override def preStart(): Unit = log.info("Device actor {}-{} started", groupId, deviceId)
  override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)

  override def receive: Receive = {
    case RequestTrackDevice(`groupId`, `deviceId`) =>
      sender() ! DeviceRegistered
    case RequestTrackDevice(groupId, deviceId) =>
      log.warning("Ignoring registration from {} - {} - This actor is responsible for {} - {}", groupId, deviceId, this.groupId, this.deviceId)
    case ReadTemperature(id) =>
      sender() ! RespondTemperature(id, lastTemperatureReading)
    case RecordTemperature(id, value) =>
      log.info("Recorded temperature reading {} = {}", id, value)
      lastTemperatureReading = Some(value)
      sender() ! TemperatureRecorded(id)
  }

}