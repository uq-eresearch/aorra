package charts.builder.csv;

import java.io.IOException;
import java.io.StringWriter;

import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

public class Csv {

  // org.supercsv.io.CsvListWriter is not thread-safe so please use this wrapper, see issue #157
  public static synchronized String write(CsvWriter writer) {
    final StringWriter sw = new StringWriter();
    if(writer != null) {
      try {
        final CsvListWriter csv = new CsvListWriter(sw,
            CsvPreference.STANDARD_PREFERENCE);
        writer.write(csv);
        csv.close();
        return sw.toString();
      } catch(IOException e) {
        // How on earth would you get an IOException with a StringWriter?
        throw new RuntimeException(e);
      }
    } else {
      return null;
    }
  }

}
