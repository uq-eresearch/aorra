package charts.representations;

import java.io.UnsupportedEncodingException;

public enum Format {
  CSV ("text/csv"),
  DOCX ("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
  EMF ("image/x-emf"),
  HTML ("text/html"),
  PNG ("image/png"),
  SVG ("image/svg+xml"),
  EPS ("image/x-eps");

  private String mimeType;

  private Format(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getMimeType() {
    return mimeType;
  }

  public Representation createRepresentation(final String content) {
    final String ct = String.format("%s; charset=utf-8", this.getMimeType());
    return new Representation() {
      @Override
      public byte[] getContent() {
        try {
          return content.getBytes("UTF-8");
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
    final String ct = this.getMimeType();
    return new Representation() {
      @Override
      public byte[] getContent() {
        return content;
      }
      @Override
      public String getContentType() {
        return ct;
      }
    };
  }

}