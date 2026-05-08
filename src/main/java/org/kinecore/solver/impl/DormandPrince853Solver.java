package org.kinecore.solver.impl;

import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.kinecore.engine.CompartmentalNetwork;
import org.kinecore.solver.ODESolver;

/**
 * Adaptive-step ODE solver using the Dormand-Prince 8(5,3) algorithm.
 */
public class DormandPrince853Solver implements ODESolver {
    private final double minStep;
    private final double maxStep;
    private final double relativeTolerance;
    private final double absoluteTolerance;

    /**
     * Constructs a solver.
     * @param minStep minimum step size
     * @param maxStep maximum step size
     * @param relTol relative tolerance
     * @param absTol absolute tolerance
     */
    public DormandPrince853Solver(double minStep, double maxStep, double relTol, double absTol) {
        this.minStep = minStep;
        this.maxStep = maxStep;
        this.relativeTolerance = relTol;
        this.absoluteTolerance = absTol;
    }

    @Override
    public void integrate(CompartmentalNetwork network, double t0, double t1,
                          double[] y0, StepHandler stepHandler) throws Exception {
        DormandPrince853Integrator integrator = new DormandPrince853Integrator(
                minStep, maxStep, relativeTolerance, absoluteTolerance);
        if (stepHandler != null) {
            integrator.addStepHandler(stepHandler);
        }
        integrator.integrate(network, t0, y0, t1, y0);
    }
}
