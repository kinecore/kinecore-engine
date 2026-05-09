package org.kinecore.engine.stochastic;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.kinecore.engine.SourceSink;

import java.util.Map;

/**
 * A {@link SourceSink} that injects continuous Gaussian (white-noise) shocks into
 * a compartment at every ODE evaluation step, modelling aleatoric uncertainty such
 * as sudden economic recessions, pandemic waves, or climate shocks.
 *
 * <h2>Reproducibility</h2>
 * <p>Rather than sharing a single PRNG across threads, each call site should construct
 * a dedicated {@code GaussianNoiseSource} seeded with a combination of the Monte Carlo
 * iteration number and the compartment index. This guarantees that every run is fully
 * deterministic and reproducible while remaining thread-safe without synchronisation.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * // Inside a MonteCarloEnsemble iteration, per-compartment:
 * long seed = iterationIndex * 31L + compartmentIndex;
 * GaussianNoiseSource noise = new GaussianNoiseSource(0.0, volatility, seed);
 * builder.addSource("Population", noise);
 * }</pre>
 *
 * <h2>Mathematical note</h2>
 * <p>The shock injected at each ODE step is drawn from N(mean, stdDev²).
 * For a diffusion coefficient σ and a step size Δt, set {@code stdDev = σ * sqrt(Δt)}
 * to approximate Itô-Euler integration of an SDE.</p>
 */
public class GaussianNoiseSource implements SourceSink {

    private final double mean;
    private final double stdDev;
    private final RandomGenerator rng;

    /**
     * Constructs a GaussianNoiseSource using a deterministic Mersenne-Twister seed.
     *
     * @param mean   mean of the Gaussian shock (usually 0 for zero-mean noise)
     * @param stdDev standard deviation of the shock amplitude
     * @param seed   deterministic seed — combine iteration and compartment indices
     *               to ensure reproducibility across Monte Carlo runs
     */
    public GaussianNoiseSource(double mean, double stdDev, long seed) {
        this.mean   = mean;
        this.stdDev = stdDev;
        this.rng    = new MersenneTwister(seed);
    }

    /**
     * Returns the mean of the Gaussian distribution.
     * @return mean
     */
    public double getMean()   { return mean; }

    /**
     * Returns the standard deviation of the Gaussian distribution.
     * @return standard deviation
     */
    public double getStdDev() { return stdDev; }

    /**
     * Draws a single Gaussian shock N(mean, stdDev²).
     *
     * <p><b>Thread-safety note:</b> each {@code GaussianNoiseSource} instance owns its
     * own PRNG state. Do not share instances across threads.</p>
     *
     * @param t      current simulation time (unused — noise is stationary)
     * @param state  full system state vector
     * @param params simulation parameters (may be inspected to scale volatility)
     * @return       a single random shock value
     */
    @Override
    public double getNetFlow(double t, double[] state, Map<String, Double> params) {
        return mean + stdDev * rng.nextGaussian();
    }
}
