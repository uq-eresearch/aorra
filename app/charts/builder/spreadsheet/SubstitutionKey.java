package charts.builder.spreadsheet;


public class SubstitutionKey {

  public static interface Val {
    public String value(Context ctx);
  }

  public static class SubstitutionException extends RuntimeException {
    public SubstitutionException(String msg, SubstitutionKey key) {
      super(msg+" "+key.toString());
    }
  }

  private final String name;

  private final String description;

  private final Val resolver;

  public SubstitutionKey(String name, String description, Val resolver) {
    this.name = name;
    this.description = description;
    this.resolver = resolver;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getValue(Context ctx) {
    return this.resolver.value(ctx);
  }

  @Override
  public String toString() {
    return getName();
  }

}
