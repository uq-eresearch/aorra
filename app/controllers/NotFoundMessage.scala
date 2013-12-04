package controllers

import play.api.http.MediaRange
import scala.collection.JavaConversions.asScalaBuffer

case class NotFoundMessage(val mimeType: String, resourcePath: String) {

  def inputStream: java.io.InputStream =
    getClass.getResourceAsStream(resourcePath)

}

object NotFoundMessage {

  lazy val formats: Seq[NotFoundMessage] = {
    val prefix = "/notfound/not_found."
    Seq(
      NotFoundMessage("text/plain", prefix+"txt"),
      NotFoundMessage("image/svg", prefix+"svg"),
      NotFoundMessage("image/webp", prefix+"webp"),
      NotFoundMessage("image/png", prefix+"png")
    )
  }

  def from(mediaRanges: java.util.List[MediaRange]): NotFoundMessage =
    apply(asScalaBuffer(mediaRanges).toSeq)

  def apply(mediaRanges: Seq[MediaRange]): NotFoundMessage = mediaRanges match {
    case Nil => formats.head
    case _ => {
      val mediaRange = mediaRanges.head
      formats.find(nf => mediaRange.accepts(nf.mimeType))
             .getOrElse(apply(mediaRanges.tail))
    }
  }

}