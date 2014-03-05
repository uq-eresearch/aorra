package charts.builder.spreadsheet.external

import java.io.InputStream
import java.io.FileInputStream
import helpers.FileStoreHelper.XLSX_MIME_TYPE
import org.specs2.mutable.Specification
import test.AorraScalaHelper.FakeAorraApp
import test.AorraScalaHelper.filestore
import test.AorraTestUtils.sessionFactory
import test.AorraScalaHelper.asAdminUser
import java.io.ByteArrayInputStream

class FileStoreExternalCellRefResolverSpec extends Specification {

  object DummyCellLink extends CellLink {
    // These should never be called, so implementation doesn't matter
    def source: Selector = ???
    def destination: Selector = ???
  }

  def dummySpreadsheet: InputStream =
    new FileInputStream("test/marine.xlsx")

  "resolver" should {

    "can resolve id-based UnresolvedRef" in new FakeAorraApp {
      asAdminUser { (session, u, fh) =>
        val fh = filestore.getManager(session)
        val userId = u.getJackrabbitUserId
        val baseFileId = fh.getRoot().createFile(
            "base.xlsx", XLSX_MIME_TYPE, dummySpreadsheet).getIdentifier
        val unresolvedRefs = (1 to 3).map { (i) =>
          val file = fh.getRoot().createFile(
              s"test$i.xlsx", XLSX_MIME_TYPE, dummySpreadsheet)
          UnresolvedRef(file.getIdentifier, DummyCellLink)
        }.toSet

        val resolver = new FileStoreExternalCellRefResolver(
            sessionFactory, filestore, userId)
        val resolvedRefs: Set[ResolvedRef] =
            resolver.resolve(baseFileId, unresolvedRefs)

        resolvedRefs.foreach { (ref) =>
          ref.source must beSome
        }
      }
    }

    "can resolve name-based UnresolvedRef" in new FakeAorraApp {
      asAdminUser { (session, u, fh) =>
        val fh = filestore.getManager(session)
        val userId = u.getJackrabbitUserId
        val baseFileId = fh.getRoot().createFile(
            "base.xlsx", XLSX_MIME_TYPE, dummySpreadsheet).getIdentifier
        val unresolvedRefs = (1 to 3).map { (i) =>
          val file = fh.getRoot()
              .createFolder(i.toString)
              .createFile(s"test.xlsx", XLSX_MIME_TYPE, dummySpreadsheet)
          val relPath = file.getPath.substring(1)
          UnresolvedRef(relPath, DummyCellLink)
        }.toSet

        val resolver = new FileStoreExternalCellRefResolver(
            sessionFactory, filestore, userId)
        val resolvedRefs: Set[ResolvedRef] =
            resolver.resolve(baseFileId, unresolvedRefs)

        resolvedRefs.foreach { (ref) =>
          ref.source must beSome
        }
      }
    }

    "can resolve absolute-URL name-based UnresolvedRef" in new FakeAorraApp {
      asAdminUser { (session, u, fh) =>
        val fh = filestore.getManager(session)
        val userId = u.getJackrabbitUserId
        val baseFileId = fh.getRoot().createFile(
            "base.xlsx", XLSX_MIME_TYPE, dummySpreadsheet).getIdentifier
        val unresolvedRefs = (1 to 3).map { (i) =>
          val file = fh.getRoot()
              .createFolder(i.toString)
              .createFile(s"test.xlsx", XLSX_MIME_TYPE, dummySpreadsheet)
          val absPath = "file://C:/foo/Downloads/" + file.getPath.substring(1)
          UnresolvedRef(absPath, DummyCellLink)
        }.toSet

        val resolver = new FileStoreExternalCellRefResolver(
            sessionFactory, filestore, userId)
        val resolvedRefs: Set[ResolvedRef] =
            resolver.resolve(baseFileId, unresolvedRefs)

        resolvedRefs.foreach { (ref) =>
          ref.source must beSome
        }
      }
    }

  }
}