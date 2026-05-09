package org.kinecore.engine;

/**
 * Builds resilient, normalized data pipelines for {@link ExogenousSignal} variables.
 *
 * <p>Supports Z-score standardization (from explicit parameters, or auto-computed
 * from a dataset) and clamped lower bounds to enforce signal boundary conditions.</p>
 */
public class SignalBuilder {
    private final ExogenousSignal source;
    private boolean standardScaling = false;
    private double mean   = 0.0;
    private double stdDev = 1.0;
    private Double lowerBound = null;

    /**
     * Constructs a SignalBuilder wrapping the given raw signal.
     * @param source the raw {@link ExogenousSignal} to process
     */
    public SignalBuilder(ExogenousSignal source) {
        this.source = source;
    }

    /**
     * Enables Z-score standardization using the mean and stdDev
     * previously set by {@link #applyStandardScaling(double, double)} or
     * {@link #applyStandardScaling(double[])}. If neither has been called,
     * the defaults (mean=0, stdDev=1) are used (no-op transformation).
     *
     * @return this builder
     */
    public SignalBuilder applyStandardScaling() {
        this.standardScaling = true;
        return this;
    }

    /**
     * Applies Z-score standardization using explicitly supplied statistics.
     *
     * @param mean   population mean of the raw signal
     * @param stdDev population standard deviation of the raw signal (must be &gt; 0)
     * @return this builder
     */
    public SignalBuilder applyStandardScaling(double mean, double stdDev) {
        this.mean   = mean;
        this.stdDev = stdDev;
        this.standardScaling = true;
        return this;
    }

    /**
     * Computes mean and standard deviation from the supplied dataset and then
     * enables Z-score standardization.
     *
     * @param dataset array of raw values from which to compute statistics; must be non-null and non-empty
     * @return this builder
     */
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

    /**
     * Clamps the (possibly standardized) signal value so it never falls below {@code bound}.
     *
     * @param bound minimum output value (e.g., 0.0 to prevent negative population signals)
     * @return this builder
     */
    public SignalBuilder applyLowerBound(double bound) {
        this.lowerBound = bound;
        return this;
    }

    /**
     * Builds and returns the processed {@link ExogenousSignal}.
     *
     * @return a new {@link ExogenousSignal} that applies the configured transformations
     */
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
