package org.kinecore.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.kinecore.engine.feedback.LinearScaleFeedback;
import org.kinecore.engine.feedback.LogisticFeedback;

import java.util.Map;

/**
 * A non-linear feedback that can be applied to specific fluxes or sources.
 *
 * <p>This interface is polymorphically deserializable by Jackson via the {@code "type"}
 * discriminator field, allowing REST clients (e.g., the KineCore dashboard) to submit
 * feedback functions over a JSON API.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = LogisticFeedback.class,    name = "logistic"),
    @JsonSubTypes.Type(value = LinearScaleFeedback.class, name = "linearScale")
})
@FunctionalInterface
public interface FeedbackOperator {

    /**
     * Applies a transformation to a flow rate or source/sink value.
     *
     * @param t            current simulation time
     * @param currentFlow  the original flow rate (before this feedback is applied)
     * @param state        full system state vector (read-only)
     * @param params       parameters from the current simulation environment
     * @return             modified flow rate (must be ≥ 0)
     */
    double apply(double t, double currentFlow, double[] state, Map<String, Double> params);
}
