package org.kinecore.engine.flux;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.Flux;

import java.util.Map;

/**
 * A flux whose rate is read from the simulation parameter map at runtime.
 *
 * <p>This is the preferred flux type for Monte Carlo simulations: the actual rate
 * value is injected by the {@link org.kinecore.solver.MonteCarloComponents.ParameterSampler}
 * into the {@code params} map at each iteration, enabling full uncertainty quantification.</p>
 *
 * <p>JSON example:
 * <pre>{ "type": "paramDriven", "paramKey": "beta", "fallbackRate": 0.1 }</pre>
 * </p>
 */
public class ParameterDrivenFlux implements Flux {

    /** Key to look up in the simulation params map. */
    private final String paramKey;

    /** Fallback rate used when the key is absent from the params map. */
    private final double fallbackRate;

    /** No-arg constructor for Jackson. */
    public ParameterDrivenFlux() { this.paramKey = ""; this.fallbackRate = 0.0; }

    /**
     * Constructs a ParameterDrivenFlux.
     * @param paramKey     key to look up in the params map
     * @param fallbackRate rate used when the key is not present
     */
    @JsonCreator
    public ParameterDrivenFlux(@JsonProperty("paramKey") String paramKey,
                                @JsonProperty("fallbackRate") double fallbackRate) {
        this.paramKey     = paramKey;
        this.fallbackRate = fallbackRate;
    }

    /** @return param key */
    public String getParamKey()    { return paramKey; }

    /** @return fallback rate */
    public double getFallbackRate() { return fallbackRate; }

    @Override
    public double getFlowRate(double t, double fromValue, double[] state, Map<String, Double> params) {
        double rate = params.getOrDefault(paramKey, fallbackRate);
        return fromValue * rate;
    }
}
