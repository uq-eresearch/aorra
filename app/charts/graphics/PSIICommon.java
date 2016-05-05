package charts.graphics;

import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.axis.CategoryLabelPositions;

import com.google.common.collect.ImmutableMap;

import charts.graphics.AutoSubCategoryAxis.Border;

public class PSIICommon {

	public void configureSubCategoryAxis(AutoSubCategoryAxis cAxis, Font f) {
	  cAxis.setLowerMargin(0.01);
	  cAxis.setUpperMargin(0.01);
	  cAxis.setItemMargin(0, 1.0);
	  cAxis.setItemMargin(1, 1.0);
	  cAxis.setItemMargin(2, 0.10);
	  cAxis.setCategoryLabelConfig(0, new AutoSubCategoryAxis.CategoryLabelConfig(
	      f,2,2, Border.ALL, Color.black));
	  cAxis.setCategoryLabelConfig(1, new AutoSubCategoryAxis.CategoryLabelConfig(
	      f,2,2, Border.BETWEEN, Color.lightGray, ImmutableMap.of("Island", "Is"), true));
	  cAxis.setCategoryLabelConfig(2, new AutoSubCategoryAxis.CategoryLabelConfig(
	      CategoryLabelPositions.UP_90,f, Color.black,2.0,2.0));
	}

}
