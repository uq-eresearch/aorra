package charts.graphics;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.StatUtils;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;

public class PartitionAxisConfigurator {

    private final boolean stackValues;

    public PartitionAxisConfigurator() {
        this(false);
    }

    public PartitionAxisConfigurator(boolean stackValues) {
        this.stackValues = stackValues;
    }

    public void configurePartitions(CategoryDataset dataset, PartitionedNumberAxis axis) {
        List<List<Double>> split = split(dataset);
        List<Range> ranges = Lists.newArrayList();
        for(List<Double> list : split) {
            ranges.add(getRange(list));
        }
        List<Range> optimised = optimise(ranges);
        List<Double> sizes = getSizes(dataset, optimised);
        for(int i=0;i<optimised.size();i++) {
            Range range = optimised.get(i);
            Double size = sizes.get(i);
            axis.addPartition(new PartitionedNumberAxis.Partition(range, size));
        }
    }

    private List<Double> getSizes(CategoryDataset dataset, List<Range> ranges) {
        double[][] distribution = { {1}, {0.75, 0.25},{0.75, 0.125, 0.125},
                {0.5, 0.25, 0.125, 0.125}, {0.5, 0.125, 0.125, 0.125, 0.125}};
        List<Double> values = getValues(dataset);
        int[] size = new int[ranges.size()];
        for(double d : values) {
            for(int i = 0;i<ranges.size();i++) {
                if(ranges.get(i).contains(d)) {
                    size[i]++;
                    break;
                }
            }
        }
        List<Pair<Integer, Integer>> list = Lists.newArrayList();
        for(int i = 0;i<ranges.size();i++) {
            list.add(Pair.of(i, size[i]));
        }
        Collections.sort(list, new Comparator<Pair<Integer, Integer>> () {
            @Override
            public int compare(Pair<Integer, Integer> o1,
                    Pair<Integer, Integer> o2) {
                return o2.getRight().compareTo(o1.getRight());
            }});
        Double[] result = new Double[list.size()];
        for(int i=0;i<list.size();i++) {
            Pair<Integer, Integer> p = list.get(i);
            result[p.getLeft()] = distribution[list.size()-1][i];
        }
        return Arrays.asList(result);
    }

    private double[] toArray(List<Double> list) {
        double[] result = new double[list.size()];
        for(int i=0;i<list.size();i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private List<List<Double>> split(CategoryDataset dataset) {
        double[] population = toArray(getValues(dataset));
        double mean = StatUtils.mean(population);
        double variance = StatUtils.populationVariance(population, mean);
        double deviation = Math.sqrt(variance);
        List<Double> xl = Lists.newArrayList();
        List<Double> l = Lists.newArrayList();
        List<Double> std = Lists.newArrayList();
        List<Double> s = Lists.newArrayList();
        List<Double> xs = Lists.newArrayList();
        for(double value : population) {
            if(Math.abs(mean-value) > 2*deviation) {
                if(value>mean) xl.add(value); else xs.add(value);
            } else if(Math.abs(mean-value) > deviation) {
                if(value>mean) l.add(value); else s.add(value);
            } else {
                std.add(value);
            }
        }
        List<List<Double>> combined = Lists.newArrayList();
        if(!xs.isEmpty()) combined.add(xs);
        if(!s.isEmpty()) combined.add(s);
        if(!std.isEmpty()) combined.add(std);
        if(!l.isEmpty()) combined.add(l);
        if(!xl.isEmpty()) combined.add(xl);
        return combined;
    }

    private List<Range> optimise(List<Range> ranges) {
        return merge(expand(addZero(ranges)));
    }

    private List<Range> expand(List<Range> ranges) {
        List<Range> result = Lists.newArrayList();
        for(Range range : ranges) {
            result.add(expand(range));
        }
        return result;
    }

    private Range expand(Range range) {
        double l = Math.abs(range.getLength());
        if(DoubleMath.fuzzyEquals(l, 0.0, 0.00001)) {
            return new Range(range.getLowerBound()-1.0, range.getUpperBound()+1.0);
        }
        double expand = l*0.05;
        if(range.getLowerBound() == 0.0) {
            return new Range(range.getLowerBound(), range.getUpperBound()+expand);
        } else if(range.getUpperBound() == 0.0) {
            return new Range(range.getLowerBound()-expand, range.getUpperBound());
        } else {
            return new Range(range.getLowerBound()-expand/2.0, range.getUpperBound()+expand/2.0);
        }
    }

    private List<Range> merge(List<Range> ranges) {
        List<Range> result = Lists.newArrayList();
        for(int i =0;i<ranges.size();i++) {
            Range range = ranges.get(i);
            if((i+1)<ranges.size()) {
                Range next = ranges.get(i+1);
                if(range.intersects(next) || DoubleMath.fuzzyEquals(
                        range.getUpperBound(),next.getLowerBound(), 0.0000001)) {
                    Range merged = Range.combine(range, next);
                    result.add(merged);
                    for(int j=i+2;j<ranges.size();j++) {
                        result.add(ranges.get(j));
                    }
                    result = merge(result);
                    break;
                } else {
                    result.add(range);
                }
            } else {
                result.add(range);
            }
        }
        return result;
    }

    private boolean containsZero(List<Range> ranges) {
        boolean result = false;
        for(Range range : ranges) {
            if(range.contains(0.0)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private List<Range> addZero(List<Range> ranges) {
        List<Range> result;
        if(containsZero(ranges)) {
            result = ranges;
        } else {
            double closestToZero = Math.abs(ranges.get(0).getLowerBound());
            for(Range r : ranges) {
                closestToZero = Math.min(closestToZero, Math.abs(r.getLowerBound()));
                closestToZero = Math.min(closestToZero, Math.abs(r.getUpperBound()));
            }
            result = Lists.newArrayList();
            for(Range r : ranges) {
                if(r.getLowerBound() == closestToZero) {
                    result.add(new Range(0, r.getUpperBound()));
                } else if(r.getUpperBound() == closestToZero) {
                    result.add(new Range(r.getLowerBound(), 0));
                } else {
                    result.add(r);
                }
            }
        }
        return result;
    }

    private Range getRange(List<Double> values) {
        double min = values.get(0).doubleValue();
        double max = values.get(0).doubleValue();
        for(Double d : values) {
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        return new Range(min,max);
    }

    private List<Double> getValues(CategoryDataset dataset) {
        if(this.stackValues) {
            return getStackedValues(dataset);
        } else {
            return getValuesRegular(dataset);
        }
    }

    private List<Double> getStackedValues(CategoryDataset dataset) {
        List<Double> values = Lists.newArrayList();
        for(int col=0;col < dataset.getColumnCount();col++) {
            double stacked = 0.0;
            for(int row=0;row<dataset.getRowCount();row++) {
                Number value = dataset.getValue(row, col);
                if(value != null) {
                    double d = value.doubleValue();
                    if(d>0.0) {
                        stacked += d;
                        values.add(stacked);
                    }
                }
            }
        }
        return values;
    }

    private List<Double> getValuesRegular(CategoryDataset dataset) {
        List<Double> values = Lists.newArrayList();
        for(int col=0;col < dataset.getColumnCount();col++) {
            for(int row=0;row<dataset.getRowCount();row++) {
                Number value = dataset.getValue(row, col);
                if(value != null) {
                    values.add(value.doubleValue());
                }
            }
        }
        return values;
    }

}
