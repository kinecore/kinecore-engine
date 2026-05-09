package org.kinecore.engine.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.FeedbackOperator;

import java.util.Map;

/**
 * A feedback operator that scales a flow rate by a generalised logistic (sigmoid)
 * function evaluated on a chosen compartment.
 *
 * <p>Uses {@code stateName} for robustness against index shifting in the model JSON.</p>
 */
public class LogisticFeedback implements FeedbackOperator {

    private final String stateName;
    private final double midpoint;
    private final double steepness;
    private final double baseValue;
    private final double maxValue;
    private final String paramKey;
    
    /** Resolved index, used during simulation for performance */
    private transient int resolvedIndex = -1;

    @JsonCreator
    public LogisticFeedback(@JsonProperty("stateName")   String stateName,
                             @JsonProperty("midpoint")   double midpoint,
                             @JsonProperty("steepness")  double steepness,
                             @JsonProperty(value = "baseValue", defaultValue = "0.0") double baseValue,
                             @JsonProperty(value = "maxValue",  defaultValue = "1.0") double maxValue,
                             @JsonProperty("paramKey")   String paramKey) {
        this.stateName = stateName;
        this.midpoint   = midpoint;
        this.steepness  = steepness;
        this.baseValue  = baseValue;
        this.maxValue   = maxValue;
        this.paramKey   = paramKey;
    }

    /**
     * Internal method to resolve the compartment name to an index.
     * @param nameToIndex map from name to state index
     */
    public void resolve(Map<String, Integer> nameToIndex) {
        Integer idx = nameToIndex.get(stateName);
        if (idx == null) throw new IllegalArgumentException("Unknown stateName: " + stateName);
        this.resolvedIndex = idx;
    }

    @Override
    public double apply(double t, double currentFlow, double[] state, Map<String, Double> params) {
        int idx = (resolvedIndex != -1) ? resolvedIndex : 0;
        double x          = (idx >= 0 && idx < state.length) ? state[idx] : 0.0;
        double sigmoid    = 1.0 / (1.0 + Math.exp(-steepness * (x - midpoint)));
        double multiplier = baseValue + (maxValue - baseValue) * sigmoid;
        
        double variation = (paramKey != null && !paramKey.isEmpty())
                ? params.getOrDefault(paramKey, 1.0)
                : 1.0;
                
        // Guard against negative stochastic draws
        return currentFlow * multiplier * Math.max(0.0, variation);
    }

    @JsonProperty("stateName")
    public String getStateName() { return stateName; }

    @JsonProperty("midpoint")
    public double getMidpoint()   { return midpoint; }

    @JsonProperty("steepness")
    public double getSteepness()  { return steepness; }

    @JsonProperty("baseValue")
    public double getBaseValue()  { return baseValue; }

    @JsonProperty("maxValue")
    public double getMaxValue()   { return maxValue; }

    @JsonProperty("paramKey")
    public String getParamKey()   { return paramKey; }
}
