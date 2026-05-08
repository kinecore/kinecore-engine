package org.kinecore.engine;

import java.util.Map;

/**
 * A non-linear feedback that can be applied to specific fluxes or sources.
 */
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
