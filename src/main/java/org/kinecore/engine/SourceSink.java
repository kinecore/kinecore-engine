package org.kinecore.engine;

import java.util.Map;

/**
 * An external inflow (source) or outflow (sink) applied to a specific compartment.
 */
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
