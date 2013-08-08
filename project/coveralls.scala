/**
 * Script which generates coverage JSON for Coveralls.io
 *
 * Execute with:
 * _JAVA_OPTIONS="-Dsbt.main.class=sbt.ScriptMain" play coveralls
 */

/***
scalaVersion := "2.10.1"

libraryDependencies ++= Seq(
  "play" %% "play" % "2.1.+"
)
*/

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
import net.liftweb.json._

object CoverallJson {

  def sourceFileJson: JArray = {
    val xmlReport = NoDtdXML.loadFile("target/jacoco/jacoco.xml")
    val fileJsObjects: Seq[Option[JObject]] = 
      for (
        packageNode <- (xmlReport \\ "package");
        fileNode <- (packageNode \ "sourcefile")
      ) yield {
        // Determine filename from package & filename
        val filename = "^/?".r.replaceFirstIn(
          (packageNode \ "@name") + "/" + (fileNode \ "@name"),
          "app/")
        // Find file source (very naive)
        val source = try {
          Some(io.Source.fromFile(filename))
        } catch {
          case _: java.io.FileNotFoundException => None
        }
        // Produce JSON object
        source.map { _.mkString }.map { content: String =>
          val lineCount = source.get.reset.getLines.size
          val lineInfo = (fileNode \ "line").map({ lineNode =>
            ((lineNode \ "@nr").mkString.toInt, (lineNode \ "@ci").mkString.toInt)
          }).toMap
          JObject(List(
            JField("name", JString(filename)),
            JField("source", JString(content)),
            JField("coverage", JArray((1 until (lineCount + 1)).map { lineNo =>
              lineInfo.get(lineNo) match {
                case Some(i: Int) => JInt(i)
                case None => JNull
              }
            }.toList))
          ))
        }
      }
    JArray(fileJsObjects.flatten.toList)
  }

  def jobId = sys.env.get("TRAVIS_JOB_ID").getOrElse("unknown")

  def timestamp = {
    val df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    df.format(new java.util.Date())
  }

  override def toString = {
    val json = JObject(List(
      JField("service_job_id", JString(jobId)),
      JField("service_name", JString("travis-ci")),
      JField("source_files", sourceFileJson),
      JField("run_at", JString(timestamp))
    ))
    pretty(render(json))
  }

}
