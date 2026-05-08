package org.kinecore.engine;

import java.util.Map;

/**
 * A directed flow of mass from one compartment to another.
 */
@FunctionalInterface
public interface Flux {

    /**
     * Computes the flow rate from the source compartment at time {@code t}.
     *
     * @param t         current simulation time
     * @param fromValue current amount in the source compartment ({@code state[fromIdx]})
     * @param state     full system state vector (read-only)
     * @param params    parameters from the current simulation environment (e.g., Monte Carlo)
     * @return          amount transferred per unit time (must be ≥ 0)
     */
    double getFlowRate(double t, double fromValue, double[] state, Map<String, Double> params);
}
