package org.kinecore.engine.flux;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.Flux;

import java.util.Map;

/**
 * A flux whose rate is a fixed proportion of the source compartment value.
 *
 * <p>Flow = {@code fromValue * rate}, where {@code rate} is constant per unit time.</p>
 *
 * <p>JSON example:
 * <pre>{ "type": "constant", "rate": 0.15 }</pre>
 * </p>
 */
public class ConstantRateFlux implements Flux {

    /** Proportional transfer rate per unit time. */
    private final double rate;

    /** No-arg constructor for Jackson. */
    public ConstantRateFlux() { this.rate = 0.0; }

    /**
     * Constructs a ConstantRateFlux.
     * @param rate proportional rate per unit time
     */
    @JsonCreator
    public ConstantRateFlux(@JsonProperty("rate") double rate) {
        this.rate = rate;
    }

    /**
     * Gets the rate.
     * @return the proportional rate
     */
    public double getRate() { return rate; }

    @Override
    public double getFlowRate(double t, double fromValue, double[] state, Map<String, Double> params) {
        return fromValue * rate;
    }
}
