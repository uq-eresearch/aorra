package models.filestore;

public interface Child<T> {

  public String getPath();

  public T getParent();

}