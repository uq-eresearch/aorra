package charts.builder.spreadsheet;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import charts.builder.spreadsheet.external.UnresolvedRef;

import com.google.common.collect.ImmutableMap;

public class XlsDataSourceTest {

  private Map<String, String[]> testdata = new ImmutableMap.Builder<String, String[]>()
      .put("progress1.xls", new String[] {"Sheet1!C5", "Sheet1!D7"})
      .put("6d843435-9ee5-45a0-9187-ccedc5b223f6", new String[] {"Sheet1!C5", "Sheet1!D8"})
      .build();
  

  @Test
  public void testExternalReference() throws IOException {
    XlsDataSource ds = new XlsDataSource(new FileInputStream("test/extref.xls"));
    Set<UnresolvedRef> urefs = ds.externalReferences();
    Assert.assertNotNull(urefs);
    Assert.assertTrue(urefs.size() == testdata.size());
    for(UnresolvedRef uref : urefs) {
      String[] links = testdata.get(uref.source());
      Assert.assertNotNull(links);
      Assert.assertEquals(links[0], uref.link().source());
      Assert.assertEquals(links[1], uref.link().destination());
    }
  }

}
