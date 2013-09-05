package charts.builder;

public interface Value {
    String format(String pattern) throws Exception;
    String asString();
    Double asDouble();
    Integer asInteger();
}
