package charts.builder;

public interface DataSource {

  @SuppressWarnings("serial")
  public class MissingDataException extends Exception {
    public MissingDataException(String msg) {
      super(msg);
    }
  }

  public Value select(String selector) throws MissingDataException;

}
