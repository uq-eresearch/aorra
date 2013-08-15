package models.filestore;

import org.jcrom.JcrEntity;

public interface Child<T> extends JcrEntity {

  public String getId();

  public T getParent();

}