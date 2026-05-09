package org.kinecore.engine.feedback;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.FeedbackOperator;

import java.util.Map;

/**
 * A simple linear multiplier applied to a flow.
 * 
 * <p>Supports parameterized scaling via {@code scalarParamKey}, allowing feedback
 * intensity to be driven by Monte Carlo samplers.</p>
 */
public class LinearScaleFeedback implements FeedbackOperator {

    private final double scalar;
    private final String scalarParamKey;

    public LinearScaleFeedback(double scalar) {
        this(scalar, null);
    }

    @JsonCreator
    public LinearScaleFeedback(@JsonProperty("scalar")         double scalar,
                               @JsonProperty("scalarParamKey") String scalarParamKey) {
        this.scalar = scalar;
        this.scalarParamKey = scalarParamKey;
    }

    @Override
    public double apply(double t, double currentFlow, double[] state, Map<String, Double> params) {
        double variation = (scalarParamKey != null && !scalarParamKey.isEmpty())
                ? params.getOrDefault(scalarParamKey, 1.0)
                : 1.0;
        // Guard against negative stochastic draws
        return currentFlow * scalar * Math.max(0.0, variation);
    }

    @JsonProperty("scalar")
    public double getScalar() {
        return scalar;
    }

    @JsonProperty("scalarParamKey")
    public String getScalarParamKey() {
        return scalarParamKey;
    }
}
