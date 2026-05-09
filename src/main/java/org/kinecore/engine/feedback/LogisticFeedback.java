package org.kinecore.engine.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.FeedbackOperator;

import java.util.Map;

/**
 * A feedback operator that scales a flow rate by a logistic (sigmoid) function
 * evaluated on a chosen compartment index.
 *
 * <p>The multiplier is: {@code 1 / (1 + exp(-steepness * (state[stateIndex] - midpoint)))}</p>
 *
 * <p>JSON example:
 * <pre>{ "type": "logistic", "stateIndex": 3, "midpoint": 500000.0, "steepness": 0.00001 }</pre>
 * </p>
 */
public class LogisticFeedback implements FeedbackOperator {

    /** Index of the state variable that drives the logistic curve. */
    private final int    stateIndex;

    /** Value of the state variable at which the multiplier is 0.5. */
    private final double midpoint;

    /** Steepness (growth rate) of the curve. */
    private final double steepness;

    /** No-arg constructor for Jackson. */
    public LogisticFeedback() { this.stateIndex = 0; this.midpoint = 0; this.steepness = 1; }

    /**
     * Constructs a LogisticFeedback.
     * @param stateIndex index of the driving state variable
     * @param midpoint   midpoint of the sigmoid
     * @param steepness  steepness of the sigmoid
     */
    @JsonCreator
    public LogisticFeedback(@JsonProperty("stateIndex") int stateIndex,
                             @JsonProperty("midpoint")   double midpoint,
                             @JsonProperty("steepness")  double steepness) {
        this.stateIndex = stateIndex;
        this.midpoint   = midpoint;
        this.steepness  = steepness;
    }

    /**
     * Gets the index of the state variable driving the logistic curve.
     * @return state index
     */
    public int    getStateIndex() { return stateIndex; }
    /**
     * Gets the midpoint of the sigmoid (value at which the multiplier is 0.5).
     * @return midpoint
     */
    public double getMidpoint()   { return midpoint; }
    /**
     * Gets the steepness (growth rate) of the sigmoid curve.
     * @return steepness
     */
    public double getSteepness()  { return steepness; }

    @Override
    public double apply(double t, double currentFlow, double[] state, Map<String, Double> params) {
        double x          = (stateIndex >= 0 && stateIndex < state.length) ? state[stateIndex] : 0.0;
        double multiplier = 1.0 / (1.0 + Math.exp(-steepness * (x - midpoint)));
        return currentFlow * multiplier;
    }
}
