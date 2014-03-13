package charts.builder.spreadsheet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

import charts.builder.Value;

public class XlsxDataSourceTest extends SpreadsheetDataSourceTest {

  @Override
  SpreadsheetDataSource datasource() throws IOException {
    return datasource(new FileInputStream("test/extref.xlsx"));
  }

  @Override
  SpreadsheetDataSource datasource(InputStream in) throws IOException {
    return new XlsxDataSource(in);
  }

  @Test
  public void rangeSelectTest() throws IOException {
    SpreadsheetDataSource ds = new XlsxDataSource(new FileInputStream("test/test.xlsx"));
    testRange(ds.rangeSelect(0, 2, 2, 2), Lists.newArrayList("c1", "c2", "c3"));
    testRange(ds.rangeSelect(2, 0, 0, 0), Lists.newArrayList("a3", "a2", "a1"));
    testRange(ds.rangeSelect(1, 0, 1, 2), Lists.newArrayList("a2", "b2", "c2"));
    testRange(ds.rangeSelect(2, 2, 2, 0), Lists.newArrayList("c3", "b3", "a3"));
  }

  private void testRange(Iterable<Value> iterable, List<String> expected) {
    Assert.assertNotNull("iterable is null", iterable);
    Iterator<Value> iter = iterable.iterator();
    Assert.assertNotNull("iterator is null", iter);
    for(int i = 0;i<expected.size();i++) {
      Assert.assertTrue("expected hasNext == true", iter.hasNext());
      Assert.assertEquals(expected.get(i), iter.next().asString());
    }
    Assert.assertFalse(iter.hasNext());
    try {
      iter.next();
      Assert.fail("NoSuchElementException expected");
    } catch(NoSuchElementException e) {}
  }

}
