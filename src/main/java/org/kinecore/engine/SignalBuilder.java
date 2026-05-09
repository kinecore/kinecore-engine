package org.kinecore.engine;

/**
 * Builds resilient, normalized data pipelines for exogenous variables.
 */
public class SignalBuilder {
    private final ExogenousSignal source;
    private boolean standardScaling = false;
    private double mean = 0.0;
    private double stdDev = 1.0;
    private Double lowerBound = null;

    public SignalBuilder(ExogenousSignal source) {
        this.source = source;
    }

    public SignalBuilder applyStandardScaling() {
        this.standardScaling = true;
        return this;
    }

    public SignalBuilder applyStandardScaling(double mean, double stdDev) {
        this.mean = mean;
        this.stdDev = stdDev;
        this.standardScaling = true;
        return this;
    }

    public SignalBuilder applyStandardScaling(double[] dataset) {
        if (dataset == null || dataset.length == 0) return this;
        double sum = 0;
        for (double v : dataset) sum += v;
        this.mean = sum / dataset.length;
        
        double sqSum = 0;
        for (double v : dataset) sqSum += (v - this.mean) * (v - this.mean);
        this.stdDev = Math.max(1e-9, Math.sqrt(sqSum / dataset.length));
        
        this.standardScaling = true;
        return this;
    }

    public SignalBuilder applyLowerBound(double bound) {
        this.lowerBound = bound;
        return this;
    }

    public ExogenousSignal build() {
        return (t, ageIdx) -> {
            double v = source.getValueAt(t, ageIdx);
            if (standardScaling) {
                v = (v - mean) / stdDev;
            }
            if (lowerBound != null && v < lowerBound) {
                v = lowerBound;
            }
            return v;
        };
    }
}
