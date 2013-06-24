package models.filestore;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.nodetype.NodeType;

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

  public void setMimeType(final String mime) {
    this.data.setMimeType(mime);
  }

  public void setData(final InputStream data) {
    this.data.setDataProvider(new JcrDataProviderImpl(data));
    this.data.setLastModified(Calendar.getInstance());
  }

  public String getMimeType() {
    return this.data.getMimeType();
  }

  public JcrDataProvider getDataProvider() {
    return this.data.getDataProvider();
  }

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

  @Override
  public String toString() {
    return String.format("%s [%s]", getPath(), getId());
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

}
