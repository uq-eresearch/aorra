package charts.reference;

import java.io.InputStream;
import java.util.Date;

public interface CacheResult {

  public Date created();

  public InputStream content();

  public InputStream datasource();

  public String datasourceMimetype();

  public String datasourceExtension();

}
