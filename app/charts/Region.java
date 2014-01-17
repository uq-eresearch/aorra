package charts;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Sets;

public enum Region {

    GBR("GBR", "Great Barrier Reef"),
    CAPE_YORK("Cape York"),
    WET_TROPICS("Wet Tropics"),
    BURDEKIN("Burdekin"),
    MACKAY_WHITSUNDAY("Mackay Whitsunday"),
    FITZROY("Fitzroy"),
    BURNETT_MARY("Burnett Mary");

    private final String properName;
    private final String name;

    private Region(String name) {
      this(name, name);
    }

    private Region(String name, String properName) {
      this.name = name;
      this.properName = properName;
    }

    public String getName() {
      return name;
    }

    public String getProperName() {
      return properName;
    }

    public static Set<Region> getRegions(Set<String> names) {
      final Set<Region> s = Sets.newHashSet();
      for (Region region : values()) {
        if (names.contains(region.name)) {
          s.add(region);
        }
      }
      return s;
    }

    public static Region lookup(String s) {
      for (Region region : values()) {
        if(StringUtils.equalsIgnoreCase(region.name, s) ||
            StringUtils.equalsIgnoreCase(region.getName(), s) ||
            StringUtils.equalsIgnoreCase(region.getProperName(), s)) {
          return region;
        }
      }
      return null;
    }

}
