package org.kinecore.solver.impl;

import org.apache.commons.math3.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.kinecore.engine.CompartmentalNetwork;
import org.kinecore.solver.ODESolver;

/**
 * High-performance solver for stiff systems using the Gragg-Bulirsch-Stoer algorithm (Task 3).
 * 
 * <p>This solver is more stable for systems with widely varying time scales (e.g., fast chemical 
 * reactions mixed with slow diffusion).</p>
 */
public class GraggBulirschStoerSolver implements ODESolver {
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
    public GraggBulirschStoerSolver(double minStep, double maxStep, double relTol, double absTol) {
        this.minStep = minStep;
        this.maxStep = maxStep;
        this.relativeTolerance = relTol;
        this.absoluteTolerance = absTol;
    }

    @Override
    public void integrate(CompartmentalNetwork network, double t0, double t1,
                          double[] y0, StepHandler stepHandler) throws Exception {
        GraggBulirschStoerIntegrator integrator = new GraggBulirschStoerIntegrator(
                minStep, maxStep, relativeTolerance, absoluteTolerance);
        if (stepHandler != null) {
            integrator.addStepHandler(stepHandler);
        }
        integrator.integrate(network, t0, y0, t1, y0);
    }
}
