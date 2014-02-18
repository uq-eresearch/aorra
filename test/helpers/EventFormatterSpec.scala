package helpers

import java.util.UUID
import org.specs2.mutable.Specification
import EventFormatter.jsonMessage
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import service.EventManager
import service.filestore.FileStore

class EventFormatterSpec extends Specification {

  "Event Formatter" can {

    "produce JSON" >> {

      import EventFormatter.jsonMessage

      "incorporating node info" in {
        val eventId = randomUUID
        val event = FileStore.Events.updateFolder(randomUUID, "test")
        val msg = jsonMessage(eventId, event)

        (msg \ "id") must beJson(eventId)
        (msg \ "type") must beJson("folder:update")
        (msg \ "data") must beJson(event.info("id"))
      }

      "without node info" in {
        val eventId = randomUUID
        val event = EventManager.Event.outOfDate
        val msg: JsObject = jsonMessage(eventId, event)

        (msg \ "id") must beJson(eventId)
        (msg \ "type") must beJson("outofdate")
        msg.keys must not contain("data")
      }

    }

    "produce Server Sent Events (SSE)" >> {

      import EventFormatter.{sseMessage, ssePingMessage}

      "incorporating node info" in {
        val eventId = randomUUID
        val event = FileStore.Events.updateFolder(randomUUID, "test")
        val msg: String = sseMessage(eventId, event)
        val lines: Seq[String] = msg.split('\n')

        lines must contain(s"id: $eventId")
        lines must contain(s"event: folder:update")
        lines must contain(s"data: ${event.info("id")}")
        msg must endWith("\n\n") // Finish with empty line
      }

      "without node info" in {
        val eventId = randomUUID
        val event = EventManager.Event.outOfDate
        val msg: String = sseMessage(eventId, event)
        val lines: Seq[String] = msg.split('\n')

        lines must contain(s"id: $eventId")
        lines must contain(s"event: outofdate")
        lines must contain(s"data: $eventId") // All SSE messages need data
        msg must endWith("\n\n") // Finish with empty line
      }

      "for pings" in {
        val msg: String = ssePingMessage()
        // We want to check certain keys exist, so break into Map
        val entries: Map[String, String] = msg.split('\n').collect {
          _.split(": ") match {
            case Array(k: String, v: String, _*) => (k, v)
          }
        }.toMap
        entries must not haveKey("id")
        entries must havePair("event" -> "ping")
        entries must haveKey("data")
        msg must endWith("\n\n") // Finish with empty line
      }

    }

  }

  // JSON string representations are quoted
  private def beJson(e: String) = equalTo(JsString(e))

  private def randomUUID = UUID.randomUUID.toString()

}