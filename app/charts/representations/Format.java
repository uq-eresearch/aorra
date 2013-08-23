package charts.representations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public enum Format {
  CSV ("text/csv"),
  PNG ("image/png"),
  SVG ("image/svg+xml");

  private String mimeType;

  private Format(String mimeType) {
    this.mimeType = mimeType;
  }

  public Representation createRepresentation(final String content) {
    final String ct = String.format("%s; charset=UTF-8", this.mimeType);
    return new Representation() {
      @Override
      public InputStream getContent() {
        try {
          return new ByteArrayInputStream(content.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e); // Should never happen
        }
      }
      @Override
      public String getContentType() {
        return ct;
      }
    };
  }

  public Representation createRepresentation(final byte[] content) {
    final String ct = this.mimeType;
    return new Representation() {
      @Override
      public InputStream getContent() {
        return new ByteArrayInputStream(content);
      }
      @Override
      public String getContentType() {
        return ct;
      }
    };
  }

}