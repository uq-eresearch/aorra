package charts.builder.csv;

import java.io.IOException;

import org.supercsv.io.CsvListWriter;

public interface CsvWriter {

  public void write(CsvListWriter csv) throws IOException;

}
