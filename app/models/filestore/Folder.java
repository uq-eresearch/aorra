package models.filestore;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.nodetype.NodeType;

import org.jcrom.AbstractJcrEntity;
import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrIdentifier;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrParentNode;

import com.google.common.collect.ImmutableSortedMap;

@JcrNode(
    nodeType = NodeType.NT_UNSTRUCTURED,
    mixinTypes = {
      NodeType.MIX_CREATED,
      NodeType.MIX_REFERENCEABLE,
      NodeType.MIX_LAST_MODIFIED,
      NodeType.MIX_VERSIONABLE
    },
    classNameProperty = "className")
public class Folder extends AbstractJcrEntity implements Child<Folder> {

  private static final long serialVersionUID = 1L;

  @JcrIdentifier
  private String id;

  @JcrParentNode
  private Folder parent;

  @JcrChildNode
  private List<Folder> folders;

  @JcrChildNode
  private List<File> files;

  public Folder() {}

  public Folder(Folder parent, String name) {
    this.parent = parent;
    this.setName(name);
  }

  public String getId() {
    return id;
  }

  public Map<String, Folder> getFolders() {
    return getImmutableMap(folders);
  }

  public Map<String, File> getFiles() {
    return getImmutableMap(files);
  }

  protected <T extends AbstractJcrEntity> Map<String, T> getImmutableMap(List<T> entities) {
    if (entities == null)
      return Collections.emptyMap();
    final ImmutableSortedMap.Builder<String, T> b =
        ImmutableSortedMap.<String, T>naturalOrder();
    for (T entity : entities)
      b.put(entity.getName(), entity);
    return b.build();
  }

  @Override
  public Folder getParent() {
    return parent;
  }

  @Override
  public String toString() {
    return String.format("%s [%s]", getPath(), getId());
  }

}
