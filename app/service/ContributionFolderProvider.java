package service;

import java.util.Set;
import javax.jcr.Node;
import javax.jcr.Session;

public interface ContributionFolderProvider {

  public Set<Node> getContributionFolders(Session session);

}