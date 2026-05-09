package org.kinecore.engine.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.SourceSink;

import java.util.Map;

/**
 * A Source/Sink that calculates flow based on a weighted sum of multiple compartments.
 * 
 * <p>Uses {@code sourceNames} for robustness against index shifting in the model JSON.</p>
 */
public class AggregatedSource implements SourceSink {

    private final String[] sourceNames;
    private final double[] weights;
    private final double scale;
    private final String scaleParamKey;
    
    /** Resolved indices, used during simulation for performance */
    private transient int[] resolvedIndices;

    /**
     * Constructs an AggregatedSource with a fixed scale.
     */
    public AggregatedSource(String[] sourceNames, double[] weights, double scale) {
        this(sourceNames, weights, scale, null);
    }

    /**
     * Constructs an AggregatedSource with an optional parameter key for the scale.
     */
    @JsonCreator
    public AggregatedSource(@JsonProperty("sourceNames")   String[] sourceNames,
                            @JsonProperty("weights")       double[] weights,
                            @JsonProperty("scale")         double scale,
                            @JsonProperty("scaleParamKey")  String scaleParamKey) {
        if (sourceNames.length != weights.length) {
            throw new IllegalArgumentException("Names and weights must have identical length");
        }
        this.sourceNames = sourceNames;
        this.weights = weights;
        this.scale = scale;
        this.scaleParamKey = scaleParamKey;
    }

    /**
     * Internal method to resolve compartment names to indices.
     * @param nameToIndex map from name to state index
     */
    public void resolve(Map<String, Integer> nameToIndex) {
        this.resolvedIndices = new int[sourceNames.length];
        for (int i = 0; i < sourceNames.length; i++) {
            Integer idx = nameToIndex.get(sourceNames[i]);
            if (idx == null) throw new IllegalArgumentException("Unknown sourceName: " + sourceNames[i]);
            this.resolvedIndices[i] = idx;
        }
    }

    @Override
    public double getNetFlow(double t, double[] state, Map<String, Double> params) {
        double sum = 0;
        int[] idxs = (resolvedIndices != null) ? resolvedIndices : new int[0];
        
        for (int i = 0; i < idxs.length; i++) {
            sum += state[idxs[i]] * weights[i];
        }
        
        double variation = (scaleParamKey != null && !scaleParamKey.isEmpty()) 
                ? params.getOrDefault(scaleParamKey, 1.0) 
                : 1.0;
                
        // Guard against negative stochastic draws (The Final Seal)
        return sum * scale * Math.max(0.0, variation);
    }

    @JsonProperty("sourceNames")
    public String[] getSourceNames() { return sourceNames; }

    @JsonProperty("weights")
    public double[] getWeights() { return weights; }

    @JsonProperty("scale")
    public double getScale() { return scale; }

    @JsonProperty("scaleParamKey")
    public String getScaleParamKey() { return scaleParamKey; }
}
