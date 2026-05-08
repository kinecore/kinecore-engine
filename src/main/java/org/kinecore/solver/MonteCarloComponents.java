package org.kinecore.solver;

import java.util.Map;

/**
 * Modular components for the Monte Carlo pipeline.
 */
public interface MonteCarloComponents {

    /**
     * Interface for sampling parameters.
     */
    @FunctionalInterface
    interface ParameterSampler {
        /**
         * Samples parameters based on a seed.
         * @param seed the random seed
         * @return a map of parameters
         */
        Map<String, Double> sample(long seed);
    }

    /**
     * Interface for running a single deterministic simulation.
     */
    @FunctionalInterface
    interface DeterministicRunner {
        /**
         * Runs a single simulation.
         * @param network the network
         * @param solver the ODE solver
         * @param t0 start time
         * @param t1 end time
         * @param stepSize step size
         * @param aggregators output aggregators
         * @return a map of trajectories
         */
        Map<String, double[]> run(org.kinecore.engine.CompartmentalNetwork network, ODESolver solver,
                                 double t0, double t1, double stepSize,
                                 Map<String, java.util.function.Function<double[], Double>> aggregators);
    }
}
