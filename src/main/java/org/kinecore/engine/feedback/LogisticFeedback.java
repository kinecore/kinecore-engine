package org.kinecore.engine.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.FeedbackOperator;

import java.util.Map;

/**
 * A feedback operator that scales a flow rate by a generalised logistic (sigmoid)
 * function evaluated on a chosen compartment index.
 *
 * <p>The multiplier ranges from {@code baseValue} (when stress is far below
 * the midpoint) to {@code maxValue} (when stress is far above the midpoint):
 * <pre>
 *   multiplier = baseValue + (maxValue - baseValue)
 *                            / (1 + exp(-steepness * (state[stateIndex] - midpoint)))
 * </pre>
 * This mirrors the full behaviour of
 * {@link org.kinecore.engine.FeedbackFactory.LogisticCurveBuilder}, ensuring the
 * Java API and the JSON REST API have identical expressive power.</p>
 *
 * <p>JSON example (Youth-Flight: 1x baseline, up to 5x at extreme stress):
 * <pre>
 * {
 *   "type"       : "logistic",
 *   "stateIndex" : 3,
 *   "midpoint"   : 500000.0,
 *   "steepness"  : 0.00001,
 *   "baseValue"  : 1.0,
 *   "maxValue"   : 5.0
 * }
 * </pre>
 * </p>
 *
 * <p>Omitting {@code baseValue} / {@code maxValue} from JSON defaults to the
 * classic 0-to-1 sigmoid (backwards-compatible with v1.0.3 payloads).</p>
 */
public class LogisticFeedback implements FeedbackOperator {

    /** Index of the state variable that drives the logistic curve. */
    private final int    stateIndex;

    /** Value of the state variable at which the multiplier is halfway between base and max. */
    private final double midpoint;

    /** Steepness (growth rate) of the sigmoid curve. */
    private final double steepness;

    /**
     * Minimum multiplier applied when the driving variable is far below the midpoint.
     * Defaults to {@code 0.0} (classic sigmoid lower bound).
     */
    private final double baseValue;

    /**
     * Maximum multiplier approached when the driving variable is far above the midpoint.
     * Defaults to {@code 1.0} (classic sigmoid upper bound).
     */
    private final double maxValue;

    /**
     * No-arg constructor for Jackson.
     * Defaults reproduce the classic 0-to-1 sigmoid for backwards compatibility.
     */
    public LogisticFeedback() {
        this.stateIndex = 0;
        this.midpoint   = 0;
        this.steepness  = 1;
        this.baseValue  = 0.0;
        this.maxValue   = 1.0;
    }

    /**
     * Constructs a LogisticFeedback with full generalised-sigmoid parameters.
     *
     * @param stateIndex index of the driving state variable in the system state vector
     * @param midpoint   state value at which the multiplier is halfway between base and max
     * @param steepness  steepness of the transition (higher = sharper threshold)
     * @param baseValue  minimum multiplier (typically 1.0 for a pass-through baseline)
     * @param maxValue   maximum multiplier (e.g., 5.0 to model 5x amplification at peak stress)
     */
    @JsonCreator
    public LogisticFeedback(@JsonProperty("stateIndex") int    stateIndex,
                             @JsonProperty("midpoint")   double midpoint,
                             @JsonProperty("steepness")  double steepness,
                             @JsonProperty(value = "baseValue", defaultValue = "0.0") double baseValue,
                             @JsonProperty(value = "maxValue",  defaultValue = "1.0") double maxValue) {
        this.stateIndex = stateIndex;
        this.midpoint   = midpoint;
        this.steepness  = steepness;
        this.baseValue  = baseValue;
        this.maxValue   = maxValue;
    }

    /**
     * Gets the index of the state variable driving the logistic curve.
     * @return state index
     */
    public int    getStateIndex() { return stateIndex; }

    /**
     * Gets the midpoint of the sigmoid (state value at which multiplier is halfway between base and max).
     * @return midpoint
     */
    public double getMidpoint()   { return midpoint; }

    /**
     * Gets the steepness (growth rate) of the sigmoid curve.
     * @return steepness
     */
    public double getSteepness()  { return steepness; }

    /**
     * Gets the minimum multiplier (lower asymptote of the sigmoid).
     * @return base multiplier value
     */
    public double getBaseValue()  { return baseValue; }

    /**
     * Gets the maximum multiplier (upper asymptote of the sigmoid).
     * @return max multiplier value
     */
    public double getMaxValue()   { return maxValue; }

    @Override
    public double apply(double t, double currentFlow, double[] state, Map<String, Double> params) {
        double x          = (stateIndex >= 0 && stateIndex < state.length) ? state[stateIndex] : 0.0;
        double sigmoid    = 1.0 / (1.0 + Math.exp(-steepness * (x - midpoint)));
        double multiplier = baseValue + (maxValue - baseValue) * sigmoid;
        return currentFlow * multiplier;
    }
}
