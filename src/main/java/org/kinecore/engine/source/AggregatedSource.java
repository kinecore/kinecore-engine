package org.kinecore.engine.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.SourceSink;

import java.util.Map;

/**
 * A Source/Sink that calculates flow based on a weighted sum of multiple compartments.
 * 
 * <p>Supports parameterized scaling via {@code scaleParamKey}, allowing birth rates
 * or migration scalars to be sampled in Monte Carlo iterations.</p>
 */
public class AggregatedSource implements SourceSink {

    private final int[] sourceIndices;
    private final double[] weights;
    private final double scale;
    private final String scaleParamKey;

    /**
     * Constructs an AggregatedSource with a fixed scale.
     */
    public AggregatedSource(int[] sourceIndices, double[] weights, double scale) {
        this(sourceIndices, weights, scale, null);
    }

    /**
     * Constructs an AggregatedSource with an optional parameter key for the scale.
     */
    @JsonCreator
    public AggregatedSource(@JsonProperty("sourceIndices") int[] sourceIndices,
                            @JsonProperty("weights")       double[] weights,
                            @JsonProperty("scale")         double scale,
                            @JsonProperty("scaleParamKey")  String scaleParamKey) {
        if (sourceIndices.length != weights.length) {
            throw new IllegalArgumentException("Indices and weights must have identical length");
        }
        this.sourceIndices = sourceIndices;
        this.weights = weights;
        this.scale = scale;
        this.scaleParamKey = scaleParamKey;
    }

    @Override
    public double getNetFlow(double t, double[] state, Map<String, Double> params) {
        double sum = 0;
        for (int i = 0; i < sourceIndices.length; i++) {
            sum += state[sourceIndices[i]] * weights[i];
        }
        
        double currentScale = (scaleParamKey != null && !scaleParamKey.isEmpty()) 
                ? params.getOrDefault(scaleParamKey, scale) 
                : scale;
                
        return sum * currentScale;
    }

    @JsonProperty("sourceIndices")
    public int[] getSourceIndices() { return sourceIndices; }

    @JsonProperty("weights")
    public double[] getWeights() { return weights; }

    @JsonProperty("scale")
    public double getScale() { return scale; }

    @JsonProperty("scaleParamKey")
    public String getScaleParamKey() { return scaleParamKey; }
}
