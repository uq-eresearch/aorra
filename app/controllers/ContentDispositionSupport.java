package controllers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

public class ContentDispositionSupport {

  private static final String SHORT = 
      "attachment; filename=\"%s\";";
  private static final String FULL = 
      "attachment; filename=\"%s\"; filename*=UTF-8''%s";

  public static String attachment(String filename) {
    final String fn_iso8859_1;
    try {
      fn_iso8859_1 = new String(filename.getBytes("ISO-8859-1"), "ISO-8859-1");
    } catch (UnsupportedEncodingException e) {
      return String.format(SHORT, filename);
    }
    final String fn_urlencoded;
    try {
      fn_urlencoded = new URI(null, filename, null).toASCIIString();
      return String.format(FULL, fn_iso8859_1, fn_urlencoded);
    } catch(URISyntaxException e) {
      return String.format(SHORT, fn_iso8859_1);
    }
  }

}
