package models.filestore;

import static org.fest.assertions.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

public class FileTest {

  @Test
  public void createDigest() {
    final String name = "test.txt";
    final String mime = "text/plain";
    final InputStream data =
        new ByteArrayInputStream("Test content.".getBytes());
    final String expectedSHA512 =
        "4e166f63a5a427c0c76c756642a4743f13635a575cd990f11069295a55785d49"+
        "b9a3cc0e5ee1bafa79d5252d03abcc19548d00ae0db20af391fc1e8e3156d606";
    File f = new File(null, name, mime, data);
    assertThat(f.getDigest()).isEqualTo(expectedSHA512);
  }

}
