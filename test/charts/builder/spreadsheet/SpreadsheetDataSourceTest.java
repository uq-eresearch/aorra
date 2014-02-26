package charts.builder.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import scala.Option;
import charts.builder.spreadsheet.external.ResolvedRef;
import charts.builder.spreadsheet.external.UnresolvedRef;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public abstract class SpreadsheetDataSourceTest {

  private final Map<String, String[]> extrefs = new ImmutableMap.Builder<String, String[]>()
      .put("progress1.xls", new String[] {"bar!C7", "foo!D7"})
      .put("6d843435-9ee5-45a0-9187-ccedc5b223f6", new String[] {"bar!C8", "foo!D8"})
      .put("progress2.xlsx", new String[] {"bar!C9", "foo!D9"})
      .put("progress1.xlsx", new String[] {"bar!A1", "O'Brien's Sales!E3"})
      .put("progress3.xlsx", new String[] {"O'Brien's!A1", "O'Brien's Sales!E4"})
      .put("groundcover below 50%.xlsx", new String[] {"areaBelow50_ordered!Z28", "O'Brien's Sales!A5"})
      .build();

  abstract SpreadsheetDataSource datasource() throws IOException;

  abstract SpreadsheetDataSource datasource(InputStream in) throws IOException;

  public void testExternalReference(SpreadsheetDataSource ds) throws IOException {
    Set<UnresolvedRef> urefs = ds.externalReferences();
    Assert.assertNotNull(urefs);
    Assert.assertEquals(extrefs.size(), urefs.size());
    for(UnresolvedRef uref : urefs) {
      String[] links = extrefs.get(uref.source());
      Assert.assertNotNull(String.format("%s not found in testdata", uref.source()), links);
      Assert.assertEquals(links[0], uref.link().source());
      Assert.assertEquals(links[1], uref.link().destination());
    }
  }

  public void testUpdateExternalReferences(SpreadsheetDataSource ds) throws IOException {
    InputStream in = updateExternalRefs(ds);
    Assert.assertNotNull(in);
    SpreadsheetDataSource ds2 = datasource(in);
    Assert.assertNull(updateExternalRefs(ds2));
    Assert.assertNull(updateExternalRefs(ds));
  }

  private InputStream updateExternalRefs(SpreadsheetDataSource ds) throws IOException {
    Set<ResolvedRef> refs = resolve(ds, ds.externalReferences());
    return ds.updateExternalReferences(refs);
  }

  private Set<ResolvedRef> resolve(final SpreadsheetDataSource ds, Set<UnresolvedRef> urefs) {
    Set<ResolvedRef> refs = Sets.newHashSet();
    for(UnresolvedRef uref : urefs) {
      refs.add(new ResolvedRef(new Option<SpreadsheetDataSource>() {

        @Override
        public int productArity() {
          throw new RuntimeException("not implemented");
        }

        @Override
        public Object productElement(int arg0) {
          throw new RuntimeException("not implemented");
        }

        @Override
        public boolean canEqual(Object arg0) {
          throw new RuntimeException("not implemented");
        }

        @Override
        public SpreadsheetDataSource get() {
          return ds;
        }

        @Override
        public boolean isEmpty() {
          return false;
        }}, uref.link()));
    }
    return refs;
  }

  @Test
  public void testExternalReference() throws IOException {
    testExternalReference(datasource());
  }

  @Test
  public void testUpdateExternalReferences() throws IOException {
    testUpdateExternalReferences(datasource());
  }
}
