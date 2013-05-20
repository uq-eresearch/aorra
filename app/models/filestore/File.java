package models.filestore;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.nodetype.NodeType;

import org.jcrom.JcrDataProviderImpl;
import org.jcrom.JcrFile;
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
public class File extends JcrFile implements Child<Folder> {

  private static final long serialVersionUID = 1L;

  @JcrIdentifier
  private String id;

  @JcrVersionName
  private String version;

  @JcrVersionCreated
  private Date updatedAt;

  @JcrParentNode
  private Folder parent;

  public File() {
    super();
  }

  public File(String name, String mime, InputStream data) {
    super();
    this.setName(name);
    this.setMimeType(mime);
    this.setLastModified(Calendar.getInstance());
    this.setDataProvider(new JcrDataProviderImpl(data));
  }

  public void setData(final InputStream data) {
    this.setDataProvider(new JcrDataProviderImpl(data));
  }

  public String getId() {
    return id;
  }

  @Override
  public Folder getParent() {
    return parent;
  }

}
