package models.filestore;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.nodetype.NodeType;

import org.jcrom.AbstractJcrEntity;
import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrFileNode;
import org.jcrom.annotations.JcrIdentifier;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrParentNode;

@JcrNode(
    nodeType = NodeType.NT_UNSTRUCTURED,
    mixinTypes = {
      NodeType.MIX_REFERENCEABLE,
      NodeType.MIX_LAST_MODIFIED
    },
    classNameProperty = "className")
public class Folder extends AbstractJcrEntity implements Child<Folder> {

  private static final long serialVersionUID = 1L;

  @JcrIdentifier
  private String id;

  @JcrParentNode
  private Folder parent;

  @JcrChildNode
  private Map<String, Object> folders;

  @JcrChildNode
  private Map<String, Object> files;

  public Folder() {}

  public Folder(String name) {
    this.setName(name);
  }

  public String getId() {
    return id;
  }

  public Map<String, Object> getFolders() {
    if (folders == null)
      folders = new TreeMap<String, Object>();
    return folders;
  }

  public Map<String, Object> getFiles() {
    if (files == null)
      files = new TreeMap<String, Object>();
    return files;
  }

  @Override
  public Folder getParent() {
    return parent;
  }

}
