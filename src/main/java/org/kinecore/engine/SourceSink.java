package org.kinecore.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.kinecore.engine.stochastic.GaussianNoiseSource;
import org.kinecore.engine.source.AggregatedSource;

import java.util.Map;

/**
 * An external inflow (source) or outflow (sink) applied to a specific compartment.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = GaussianNoiseSource.class, name = "gaussianNoise"),
    @JsonSubTypes.Type(value = AggregatedSource.class,    name = "aggregatedSource")
})
@FunctionalInterface
public interface SourceSink {

    /**
     * Computes the net addition to the compartment at time {@code t}.
     *
     * @param t        current simulation time
     * @param state    full system state vector (read-only)
     * @param params   parameters from the current simulation environment (e.g., Monte Carlo)
     * @return         net addition per unit time (positive = source, negative = sink)
     */
    double getNetFlow(double t, double[] state, Map<String, Double> params);
}
