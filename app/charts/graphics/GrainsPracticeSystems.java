package charts.graphics;

import java.awt.Color;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;

public class GrainsPracticeSystems extends LandPracticeSystems {

  public GrainsPracticeSystems() {
    super(COLOR_A_TRANS, COLOR_B_TRANS, COLOR_CD_TRANS, COLOR_A, COLOR_B, COLOR_CD);
  }

  @Override
  protected List<Pair<String, Color>> getLegend() {
    return ImmutableList.of(
        Pair.of("A", COLOR_A), Pair.of("B", COLOR_B),
        Pair.of("C/D", COLOR_CD));
  }
}
