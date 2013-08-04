package charts.spreadsheet;


public interface DataSource {

    public Value select(String selector) throws Exception;

}
