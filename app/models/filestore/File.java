package models.filestore;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import javax.jcr.nodetype.NodeType;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.jcrom.AbstractJcrEntity;
import org.jcrom.JcrDataProvider;
import org.jcrom.JcrDataProviderImpl;
import org.jcrom.JcrFile;
import org.jcrom.annotations.JcrBaseVersionName;
import org.jcrom.annotations.JcrFileNode;
import org.jcrom.annotations.JcrIdentifier;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrParentNode;
import org.jcrom.annotations.JcrProperty;
import org.jcrom.annotations.JcrProtectedProperty;
import org.jcrom.annotations.JcrVersionName;

import play.api.libs.Files.TemporaryFile;
import play.api.libs.Files.TemporaryFile$;

@JcrNode(
    nodeType = NodeType.NT_UNSTRUCTURED,
    mixinTypes = {
      NodeType.MIX_CREATED,
      NodeType.MIX_LAST_MODIFIED,
      NodeType.MIX_REFERENCEABLE,
      NodeType.MIX_VERSIONABLE
    },
    classNameProperty = "className")
public class File extends AbstractJcrEntity implements Child<Folder> {

  private static final long serialVersionUID = 1L;

  @JcrIdentifier
  protected String id;

  /**
   * Immediate predecessor of the next version created.
   * (In this case, also the latest version.)
   */
  @JcrBaseVersionName
  protected String latestVersion;

  @JcrVersionName
  protected String version;

  @JcrProtectedProperty(name="jcr:created")
  protected Calendar created;

  @JcrProtectedProperty(name="jcr:createdBy")
  protected String createdBy;

  @JcrProperty(name="jcr:lastModified")
  protected Calendar lastModified;

  @JcrProperty(name="jcr:lastModifiedBy")
  protected String lastModifiedBy;

  @JcrProperty(name="sha512")
  protected byte[] digest = new byte[512/8];

  @JcrParentNode
  protected Folder parent;

  @JcrFileNode
  protected JcrFile data;

  public File() {
    super();
  }

  public File(Folder folder, String name, String mime, InputStream data) {
    super();
    this.parent = folder;
    this.setName(name);
    this.data = new JcrFile();
    this.data.setName(name);
    this.setMimeType(mime);
    this.setData(data);
  }

  protected InputStream bufferAndDigest(InputStream data) {
    final MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      // Should never happen
      throw new RuntimeException(e);
    }
    final TemporaryFile tf = TemporaryFile$.MODULE$.apply("aorraTempData", "");
    try {
      FileUtils.copyInputStreamToFile(new DigestInputStream(data, md), tf.file());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.digest = md.digest();
    try {
      return new FileInputStream(tf.file()) {
        // Reference to couple FileInputStream lifetime to TemporaryFile
        @SuppressWarnings("unused")
        private final TemporaryFile temporaryFile = tf;
      };
    } catch (FileNotFoundException e) {
      // Rather unlikely to occur - certainly not recoverable
      throw new RuntimeException(e);
    }
  }


  public void setMimeType(final String mime) {
    this.data.setMimeType(mime);
  }

  public void setData(final InputStream data) {
    this.data.setDataProvider(new JcrDataProviderImpl(bufferAndDigest(data)));
    this.data.setLastModified(Calendar.getInstance());
  }

  public String getMimeType() {
    return this.data.getMimeType();
  }

  public JcrDataProvider getDataProvider() {
    return this.data.getDataProvider();
  }

  @Override
  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }

  public String getLatestVersion() {
    return version;
  }

  @Override
  public Folder getParent() {
    return parent;
  }

  protected void setDigest(byte[] digest) {
    this.digest = digest;
  }

  public String getDigest() {
    return new HexBinaryAdapter().marshal(digest).toLowerCase();
  }

  @Override
  public String toString() {
    return String.format("%s [%s] (%s)", getPath(), getId(), getDigest());
  }

  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public Calendar getLastModified() {
    return lastModified;
  }

  public void setLastModified(String userId) {
    this.lastModifiedBy = userId;
    this.lastModified = Calendar.getInstance();
  }

  public boolean containsSameDataAs(File other) {
    if (other == null)
      return false;
    return new EqualsBuilder()
      .append(getDigest(), other.getDigest())
      .append(getLastModified(), other.getLastModified())
      .isEquals();
  }

}
