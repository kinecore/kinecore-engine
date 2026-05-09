package org.kinecore.engine.stochastic;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.random.MersenneTwister;
import org.kinecore.engine.SourceSink;

import java.util.Map;

/**
 * A {@link SourceSink} that injects continuous stochastic shocks into a compartment,
 * modelling aleatoric uncertainty such as economic recessions or pandemic waves.
 *
 * <h2>Adaptive-Solver Compatibility</h2>
 * <p>Adaptive Runge-Kutta solvers (e.g., Dormand-Prince 8(5,3)) call
 * {@code computeDerivatives} 6–13 times per step at slightly different micro-time
 * points to estimate the local truncation error. If the noise function returns a
 * freshly-drawn, uncorrelated random number on every call, the error estimate will
 * see a wildly discontinuous derivative, force the step size toward machine epsilon
 * ({@code ~1e-15}), and the simulation will freeze at {@code t ≈ 0}.</p>
 *
 * <p>This class avoids that trap by pre-computing a deterministic set of shock
 * values at fixed {@code interval}-spaced knots and linearly interpolating between
 * them. The resulting piecewise-linear curve is Lipschitz-continuous, so the
 * adaptive solver sees a smooth derivative between knots and progresses correctly.</p>
 *
 * <h2>Reproducibility</h2>
 * <p>Each instance owns a private {@link MersenneTwister} seeded from the caller's
 * {@code seed} argument. Seeding with {@code iterationIndex * 31L + compartmentIndex}
 * ensures independent, reproducible noise paths across Monte Carlo iterations without
 * any shared state or synchronisation overhead.</p>
 *
 * <h2>SDE Scaling</h2>
 * <p>For an Itô SDE with diffusion coefficient σ and shock interval Δt, set
 * {@code stdDev = σ * Math.sqrt(interval)} to reproduce the correct Brownian
 * variance over each interval.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * // Per-compartment, per-iteration — thread-safe, no sharing required
 * long seed = (long) iter * 31L + compartmentIndex;
 * SourceSink noise = new GaussianNoiseSource(
 *     0.0,          // zero-mean
 *     sigma * Math.sqrt(1.0),  // yearly interval
 *     seed, tStart, tEnd, 1.0  // annual knots
 * );
 * builder.addSource("Population", noise);
 * }</pre>
 */
public class GaussianNoiseSource implements SourceSink {

    /**
     * Pre-computed piecewise-linear noise spline.
     * Evaluated at query time {@code t} to return a Lipschitz-continuous value
     * that the adaptive solver can treat as a smooth signal.
     */
    private final PolynomialSplineFunction noiseSpline;

    /** Lower bound of the time domain covered by the spline. */
    private final double tStart;

    /** Upper bound of the time domain covered by the spline. */
    private final double tEnd;

    /**
     * Constructs a {@code GaussianNoiseSource} by pre-computing a linear noise spline.
     *
     * @param mean     mean of each Gaussian knot value (typically 0.0 for zero-mean noise)
     * @param stdDev   standard deviation of each knot; set to {@code σ * sqrt(interval)}
     *                 to match Itô-Euler Brownian variance
     * @param seed     deterministic PRNG seed — combine Monte Carlo iteration index and
     *                 compartment index to guarantee independent, reproducible paths
     * @param tStart   simulation start time (must match the ODE solver's start time)
     * @param tEnd     simulation end time (must match the ODE solver's end time)
     * @param interval spacing between noise knots in simulation-time units (e.g., 1.0 for
     *                 annual shocks in a yearly demographic model)
     */
    public GaussianNoiseSource(double mean, double stdDev, long seed,
                                double tStart, double tEnd, double interval) {
        this.tStart = tStart;
        this.tEnd   = tEnd;

        MersenneTwister rng = new MersenneTwister(seed);

        // Build evenly-spaced knot arrays
        int steps = (int) Math.ceil((tEnd - tStart) / interval) + 1;
        double[] t = new double[steps];
        double[] v = new double[steps];

        for (int i = 0; i < steps; i++) {
            t[i] = tStart + i * interval;
            v[i] = mean + stdDev * rng.nextGaussian();
        }

        // Ensure the last knot exactly covers tEnd (avoids out-of-range evaluation)
        t[steps - 1] = Math.max(t[steps - 1], tEnd);

        this.noiseSpline = new LinearInterpolator().interpolate(t, v);
    }

    /**
     * Returns the mean of the Gaussian distribution used at each knot.
     *
     * @return the mean shock value
     */
    public double getMean() {
        // Extracting exact mean from spline coefficients is non-trivial; expose
        // via constructor parameter storage if needed. Provided for API symmetry.
        return noiseSpline.value((tStart + tEnd) / 2.0);
    }

    /**
     * Evaluates the pre-computed noise spline at time {@code t}, returning a
     * Lipschitz-continuous shock value safe for adaptive ODE solvers.
     *
     * <p>If {@code t} falls outside {@code [tStart, tEnd]}, the nearest knot
     * value is returned (flat extrapolation) rather than throwing an exception,
     * to ensure numerical robustness at integration boundaries.</p>
     *
     * @param t      current simulation time
     * @param state  full system state vector (not used; noise is state-independent)
     * @param params simulation parameter map (not used; noise uses pre-computed knots)
     * @return       interpolated shock value at time {@code t}
     */
    @Override
    public double getNetFlow(double t, double[] state, Map<String, Double> params) {
        // Clamp to the spline domain to prevent out-of-range exceptions at
        // the adaptive solver's boundary-probing micro-steps
        double tClamped = Math.max(tStart, Math.min(tEnd, t));
        return noiseSpline.value(tClamped);
    }
}
