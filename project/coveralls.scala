
// Handle missing DTD
import scala.xml.Elem
import scala.xml.factory.XMLLoader
import javax.xml.parsers.SAXParser
object NoDtdXML extends XMLLoader[Elem] {
  override def parser: SAXParser = {
    val f = javax.xml.parsers.SAXParserFactory.newInstance()
    f.setNamespaceAware(false)
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    f.newSAXParser()
  }
}

import scala.xml._
import scala.util.parsing.json._

case class JSONNull extends JSONObject(Map()) {
  override def toString(formatter: (Any) => String): String = "null"
  override def toString() = "null"
}

val xmlReport = NoDtdXML.loadFile("target/jacoco/jacoco.xml")

val sourceFileJson = JSONArray((xmlReport \\ "package").map({ packageNode =>
  (packageNode \ "sourcefile").map({ fileNode =>
    val filename = (packageNode \ "@name") + "/" + (fileNode \ "@name")
    // Find file source (very naive)
    val source = try { 
      Some(io.Source.fromFile("app/"+filename))
    } catch {
      case _: java.io.FileNotFoundException => None
    }
    // Produce JSON object
    source.map { _.mkString }.map { content =>
      val lineCount = source.get.reset.getLines.size
      val lineInfo = (fileNode \ "line").map({ lineNode =>
        ((lineNode \ "@nr").mkString.toInt, (lineNode \ "@ci").mkString.toInt)
      }).toMap
      JSONObject(Map(
        "name" -> filename,
        "source" -> content,
        "coverage" -> JSONArray((0 until lineCount).map { lineNo =>
          lineInfo.getOrElse(lineNo, JSONNull())
        }.toList)
      ))
    }
  })
}).toList.flatten)

println(JSONObject(Map(
  "service_job_id" -> sys.env.get("TRAVIS_JOB_ID").get,
  "service_name" -> "travis-ci",
  "source_files" -> sourceFileJson
)))
