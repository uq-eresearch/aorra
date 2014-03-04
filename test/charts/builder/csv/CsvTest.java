package charts.builder.csv;

import static java.lang.String.format;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.ChartType;
import charts.Region;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class CsvTest {

  //@Test
  public void testCsvMultiThreaded() throws InterruptedException {
    final List<Throwable> errors = Lists.newArrayList();
    final List<Thread> tlist = Lists.newArrayList();
    for(final ChartType type : ChartType.values()) {
      for(final Region r : Region.values()) {
        Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            testCsv(r, type);
          }});
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()  {
          @Override
          public void uncaughtException(Thread t, Throwable e) {
            errors.add(e);
          }
        });
        t.start();
        tlist.add(t);
      }
    }
    for(Thread t : tlist) {
      t.join();
    }
    Assert.assertTrue(errors.toString(), errors.isEmpty());
  }

  @Test
  public void testCsv() {
    for(final ChartType type : ChartType.values()) {
      for(final Region r : Region.values()) {
        testCsv(r, type);
      }
    }
  }

  private void testCsv(Region region, ChartType type) {
    Assert.assertEquals(String.format("%s %s,1,2,3,4\r\n",
        region.getProperName(), type.getLabel()), getCsv(region, type));
  }

  private String getCsv(Region region, ChartType type) {
    final StringWriter sw = new StringWriter();
    try {
      final CsvListWriter csv = new CsvListWriter(sw,
          CsvPreference.STANDARD_PREFERENCE);
      List<String> columnKeys = Lists.newArrayList("1", "2", "3", "4");
      final List<String> heading = ImmutableList.<String>builder()
          .add(format("%s %s", region.getProperName(), type.getLabel()))
          .addAll(columnKeys)
          .build();
      csv.write(heading);
      csv.close();
    } catch (IOException e) {
      // How on earth would you get an IOException with a StringWriter?
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

}
