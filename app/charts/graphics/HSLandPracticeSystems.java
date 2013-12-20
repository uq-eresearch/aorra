package charts.graphics;

import java.awt.Color;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;

public class HSLandPracticeSystems extends LandPracticeSystems {

  public HSLandPracticeSystems() {
    super(COLOR_A_TRANS, COLOR_B_TRANS, COLOR_C_TRANS, COLOR_D_TRANS,
        COLOR_A, COLOR_B, COLOR_C, COLOR_D);
  }

  @Override
  protected List<Pair<String, Color>> getLegend() {
    return ImmutableList.of(
        Pair.of("A", COLOR_A), Pair.of("B", COLOR_B),
        Pair.of("C", COLOR_C), Pair.of("D", COLOR_D));
  }

}
