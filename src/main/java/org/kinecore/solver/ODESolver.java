package org.kinecore.solver;

import org.apache.commons.math3.ode.sampling.StepHandler;
import org.kinecore.engine.CompartmentalNetwork;

/**
 * Pluggable ODE solver interface.
 */
public interface ODESolver {
    /**
     * Integrates the network from t0 to t1.
     *
     * @param network     the compartmental system
     * @param t0          start time
     * @param t1          end time
     * @param y0          initial state (will be modified to final state)
     * @param stepHandler optional step handler for recording intermediate states
     * @throws Exception if integration fails
     */
    void integrate(CompartmentalNetwork network, double t0, double t1, double[] y0,
                   StepHandler stepHandler) throws Exception;
}
