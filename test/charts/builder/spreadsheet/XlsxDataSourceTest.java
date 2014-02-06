package charts.builder.spreadsheet;

import java.io.FileInputStream;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import charts.builder.spreadsheet.external.UnresolvedRef;

public class XlsxDataSourceTest {

  @Test
  public void testExternalReferences() throws Exception {
    XlsxDataSource ds = new XlsxDataSource(new FileInputStream("test/extref.xlsx"));
    Set<UnresolvedRef> urefs = ds.externalReferences();
    Assert.assertNotNull(urefs);
    Assert.assertTrue(urefs.size() == 1);
    UnresolvedRef uref = urefs.iterator().next();
    Assert.assertEquals("external.xlsx", uref.source());
    Assert.assertEquals("bar!A1", uref.link().source());
    Assert.assertEquals("foo!A1", uref.link().destination());
  }

}
