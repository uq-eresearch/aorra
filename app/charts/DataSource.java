package charts;

public interface DataSource {

    public Value select(String selector) throws Exception;

}
