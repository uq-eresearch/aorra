package infographic

import org.jsoup.Jsoup
import java.io.InputStream
import play.api.libs.json._


object MarineCaptionsJsonBuilder {

  def apply(data: InputStream): JsValue = {
    val regions = Map(
      "gbr" -> "Great Barrier Reef",
      "cape-york" -> "Cape York",
      "wet-tropics" -> "Wet Tropics",
      "burdekin" -> "Burdekin",
      "mackay-whitsunday" -> "Mackay-Whitsunday",
      "fitzroy" -> "Fitzroy",
      "burnett-mary" -> "Burnett-Mary"
    )
    val doc = Jsoup.parse(data, "UTF-8", "/")
    JsObject(
      regions.toSeq.map { case (regionId, regionName) =>
        val searchStr = regionName.replaceAll("[\\- ]", ".")
        // Find header
        Option(doc.select("h3:matches("+searchStr+")").first)
          // Look for the next element
          .flatMap(e => Option(e.nextElementSibling))
          // It should be a <figure />
          .filter(e => e.tagName == "figure")
          // Find the caption
          .flatMap(e => Option(e.select("figcaption").first))
          // Get the text
          .map(_.text)
          // Output a pair with the text
          .map(text => (regionId, JsString(text)))
          // If it we didn't find what we were after, output nothing
          .getOrElse((regionId, JsString("")))
      }
    )
  }

}