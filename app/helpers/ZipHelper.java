package helpers;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class ZipHelper {

  public static ZipArchiveOutputStream setupZipOutputStream(final OutputStream o) {
    // ZipArchiveOutputStream writes 0 length byte arrays occasionally
    // which terminates the chunked transfer (see ArchiveAsync) so ignore the empty arrays
    ZipArchiveOutputStream out = new ZipArchiveOutputStream(new OutputStream() {

      @Override
      public void write(int b) throws IOException {
        o.write(b);
      }

      @Override
      public void close() throws IOException {
        o.close();
      }

      @Override
      public void flush() throws IOException {
        o.flush();
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        if((b.length > 0) && (len > 0)) {
          o.write(b, off, len);
        }
      }

      @Override
      public void write(byte[] b) throws IOException {
        if(b.length > 0) {
          o.write(b);
        }
      }
    });
    out.setEncoding("Cp437");
    out.setFallbackToUTF8(true);
    out.setUseLanguageEncodingFlag(true); 
    out.setCreateUnicodeExtraFields(
        ZipArchiveOutputStream.UnicodeExtraFieldPolicy.NOT_ENCODEABLE);
    return out;
  }
}
