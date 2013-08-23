package charts.representations;

import java.io.InputStream;

public interface Representation {

  public InputStream getContent();

  public String getContentType();

}
