package controllers

import org.specs2.mutable.Specification
import javax.jcr.Session
import models.User
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, HEAD, POST}
import play.api.test.Helpers.OK
import play.api.test.Helpers.contentType
import play.api.test.Helpers.header
import play.api.test.Helpers.route
import play.api.test.Helpers.status
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import test.AorraScalaHelper.FakeAorraApp
import test.AorraScalaHelper.asAdminUser
import scala.concurrent.Await
import akka.util.Timeout
import java.util.UUID
import scala.io.Source
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter._
import java.io.File
import scala.collection.JavaConversions._
import java.nio.file.Path

class InfographicControllerSpec extends Specification {

  implicit val timeout = Timeout(30000)

  "data.json requires login" >> new FakeAorraApp {
    asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
      val id = UUID.randomUUID.toString
      val Some(result) = route(
          FakeRequest(GET, s"/file/$id/infographic/data.json"))
      status(result) must equalTo(303)
      header("Location", result) must beSome("/login")
    }
  }

  "static files should exist for every entry in manifest.txt" >> new FakeAorraApp {
    val manifest = Source.fromInputStream(
      getClass.getResourceAsStream("/infographic/manifest.txt")).getLines
    val id = UUID.randomUUID.toString
    manifest.foreach { staticFile =>
      val Some(result) = route(
          FakeRequest(GET, s"/file/$id/infographic/$staticFile"))
      status(result) must equalTo(200).setMessage(s"$staticFile should exist")
    }
  }

  "static files should be in the manifest if they're" >> {

    lazy val inManifest: Path => Boolean = {
      val manifest: Set[String] = Source.fromInputStream(
        getClass.getResourceAsStream("/infographic/manifest.txt"))
          .getLines
          .toSet;
      { path: Path =>
        manifest.contains(path.toString)
      }
    }

    def resourceFilesByRegex(re: String): List[Path] = {
      val baseDir = new File("./resources/infographic")
      val files = FileUtils.listFiles(
          baseDir,
          new RegexFileFilter(re),
          TrueFileFilter.INSTANCE).toList
      files.map { file =>
        baseDir.toPath.relativize(file.toPath)
      }
    }

    "css" >> {
      resourceFilesByRegex(""".*\.css""").forall(inManifest) must beTrue
    }

    "geojson" >> {
      resourceFilesByRegex(""".*\.geojson""").forall(inManifest) must beTrue
    }

    "html" >> {
      resourceFilesByRegex(""".*\.html""").forall(inManifest) must beTrue
    }

    "js" >> {
      resourceFilesByRegex(""".*\.js""").forall(inManifest) must beTrue
    }

    "png" >> {
      resourceFilesByRegex(""".*\.png""").forall(inManifest) must beTrue
    }

    "svg" >> {
      resourceFilesByRegex(""".*\.png""").forall(inManifest) must beTrue
    }
  }

}