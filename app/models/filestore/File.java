package models.filestore;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.nodetype.NodeType;

import org.jcrom.AbstractJcrEntity;
import org.jcrom.JcrDataProvider;
import org.jcrom.JcrDataProviderImpl;
import org.jcrom.JcrFile;
import org.jcrom.annotations.JcrFileNode;
import org.jcrom.annotations.JcrIdentifier;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrParentNode;
import org.jcrom.annotations.JcrVersionCreated;
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

  @JcrVersionName
  protected String version;

  @JcrVersionCreated
  protected Date updatedAt;

  @JcrParentNode
  protected Folder parent;

  @JcrFileNode
  protected JcrFile data;

  public File() {
    super();
  }

  public File(String name, String mime, InputStream data) {
    super();
    this.setName(name);
    this.data = new JcrFile();
    this.data.setName(name);
    this.data.setLastModified(Calendar.getInstance());
    this.setMimeType(mime);
    this.setData(data);
  }

  public void setMimeType(final String mime) {
    this.data.setMimeType(mime);
  }

  public void setData(final InputStream data) {
    this.data.setDataProvider(new JcrDataProviderImpl(data));
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

  @Override
  public Folder getParent() {
    return parent;
  }

}
