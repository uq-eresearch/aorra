package service;

import java.util.Set;
import javax.jcr.Node;
import javax.jcr.Session;

public interface ContributionFolderProvider {

  public Set<Node> getAll(Session session);

  public Set<Node> getWritable(Session session);

}