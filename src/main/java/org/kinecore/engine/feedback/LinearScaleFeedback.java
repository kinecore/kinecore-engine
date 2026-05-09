package org.kinecore.engine.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.FeedbackOperator;

import java.util.Map;

/**
 * A feedback operator that multiplies a flow rate by a fixed scalar.
 *
 * <p>Useful for policy interventions (e.g., a 30% reduction in emigration rate)
 * that can be toggled on or off by changing a single parameter in a What-If scenario.</p>
 *
 * <p>JSON example:
 * <pre>{ "type": "linearScale", "scalar": 0.70 }</pre>
 * </p>
 */
public class LinearScaleFeedback implements FeedbackOperator {

    /** Multiplier applied to the incoming flow rate. */
    private final double scalar;

    /** No-arg constructor for Jackson. */
    public LinearScaleFeedback() { this.scalar = 1.0; }

    /**
     * Constructs a LinearScaleFeedback.
     * @param scalar multiplier to apply
     */
    @JsonCreator
    public LinearScaleFeedback(@JsonProperty("scalar") double scalar) {
        this.scalar = scalar;
    }

    /** @return the scalar multiplier */
    public double getScalar() { return scalar; }

    @Override
    public double apply(double t, double currentFlow, double[] state, Map<String, Double> params) {
        return currentFlow * scalar;
    }
}
