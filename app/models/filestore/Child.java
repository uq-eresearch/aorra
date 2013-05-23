package models.filestore;

import org.jcrom.JcrEntity;

public interface Child<T> extends JcrEntity {

  public T getParent();

}