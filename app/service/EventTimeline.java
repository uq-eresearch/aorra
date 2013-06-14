package service;

import java.util.NavigableMap;

public interface EventTimeline<K extends Comparable<?>, V> {

  public static class ForgottenEventException extends Exception {
    public ForgottenEventException(String string) {
      super(string);
    }
    private static final long serialVersionUID = 1L;
  }

  public abstract K getLastEventId();

  public abstract NavigableMap<K, V> getKnown();

  public abstract NavigableMap<K, V> getSince(K id)
    throws ForgottenEventException;

  public abstract K record(V event);

}