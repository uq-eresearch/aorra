package charts.builder;

public interface DataSourceFactory {

  public DataSource getDataSource(String id) throws Exception;

}
