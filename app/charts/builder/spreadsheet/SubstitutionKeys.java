package charts.builder.spreadsheet;

import charts.Region;

public class SubstitutionKeys {

  public static final SubstitutionKey CATCHMENT = new SubstitutionKey("catchment",
      "literally 'Region' for region gbr and 'Catchment' for all others",
      new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          return ctx.region() == Region.GBR ? "Region" : "Catchment";
        }
      });

}
