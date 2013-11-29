package helpers

import java.text.DateFormat
import java.util.Calendar
import service.EventManager.{Event => EmEvent}

import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.JsObject

object EventFormatter {

  private val dtFormat = DateFormat.getDateTimeInstance()

  def jsonMessage(id: String, event: EmEvent): JsObject = {
    // Response varies depending on whether event.info is null
    val body = event.info("id") match {
      case null => Json.obj("type" -> event.`type`)
      case mId => Json.obj("type" -> event.`type`, "data" -> mId)
    }
    // All responses have an ID
    Json.obj("id" -> id) ++ body
  }

  def sseMessage(id: String, event: EmEvent): String = {
    val tmpl = "id: %s\nevent: %s\ndata: %s\n\n"
    event.info("id") match {
      case null => tmpl.format(id, event.`type`, id)
      case mId => tmpl.format(id, event.`type`, mId)
    }
  }

  def ssePingMessage(): String = {
    val msg = dtFormat.format(Calendar.getInstance.getTime)
    s"event: ping\ndata: $msg\n\n"
  }

}
