package controllers

import java.awt.Dimension
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConversions.seqAsJavaList

import org.jcrom.Jcrom

import com.google.inject.Inject

import ScalaSecured.isAuthenticated
import charts.Chart.UnsupportedFormatException
import charts.Region
import charts.builder.ChartBuilder
import charts.representations.Format
import controllers.Chart.getDatasourcesFromIDs
import javax.jcr.Session
import models.CacheableUser
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Controller
import play.api.mvc.EssentialAction
import play.libs.F

class ArchiveAsync @Inject()(
      val jcrom: Jcrom,
      val filestore: service.filestore.FileStore,
      val sessionFactory: service.JcrSessionFactory)
    extends Controller {

  def chartArchive(id: String): EssentialAction = isAuthenticated { user =>
    implicit request => {
      val enumerator = Enumerator.outputStream { os =>
        val zip = new ZipOutputStream(os);
        zip.setMethod(ZipOutputStream.DEFLATED);
        zip.setLevel(5);
        addChartFilesToArchive(user, zip, id)
        zip.close()
      }
      Ok.stream(enumerator >>> Enumerator.eof).withHeaders(
        "Content-Type" -> "application/zip",
        "Content-Disposition" -> s"attachment; filename=${id}.zip",
        "Cache-Control" -> "max-age=0, must-revalidate"
      )
    }
  }

  protected def addChartFilesToArchive(
      user: CacheableUser, zos: ZipOutputStream, id: String) {
   val chartBuilder = new ChartBuilder
   inSession(user, { session =>
      val datasources = getDatasourcesFromIDs(filestore, session, Seq(id))
      datasources.foreach { case (file, datasource) =>
        val charts = chartBuilder.getCharts(
            Seq(datasource), Region.values.toSeq, new Dimension)
        charts.foreach { chart =>
          Format.values().foreach { f =>
            val filepath = String.format("%s/%s-%s-%s.%s\n",
                id,
                chart.getDescription().getType(),
                chart.getDescription().getRegion(),
                file.getIdentifier(),
                f.name()).trim().toLowerCase();
            try {
              val data = chart.outputAs(f).getContent()
              zos.putNextEntry(new ZipEntry(filepath))
              zos.write(data)
              zos.closeEntry
            } catch {
              case _: UnsupportedFormatException => // skip to next
            }
          }
        }
      }
    })
  }

  protected def inSession[A](user: CacheableUser, f: Session => A): A = {
    sessionFactory.inSession(user.getJackrabbitUserId(),
      new F.Function[Session, A] {
        override def apply(session: Session) = f(session)
      })
  }
}