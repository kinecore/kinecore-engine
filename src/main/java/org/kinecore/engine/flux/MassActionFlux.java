package org.kinecore.engine.flux;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.Flux;

import java.util.Map;

/**
 * A flux driven by mass-action kinetics: flow = rate × fromValue × state[interactorIdx].
 *
 * <p>Uses {@code interactorName} for robustness against index shifting in the model JSON.</p>
 */
public class MassActionFlux implements Flux {

    private final double rate;
    private final String interactorName;
    
    /** Resolved index, used during simulation for performance */
    private transient int resolvedInteractorIndex = -1;

    /** No-arg constructor for Jackson. */
    public MassActionFlux() { this.rate = 0.0; this.interactorName = null; }

    /**
     * Constructs a MassActionFlux.
     * @param rate            rate constant
     * @param interactorName  name of the interacting compartment
     */
    @JsonCreator
    public MassActionFlux(@JsonProperty("rate") double rate,
                          @JsonProperty("interactorName") String interactorName) {
        this.rate           = rate;
        this.interactorName = interactorName;
    }

    /**
     * Internal method to resolve the interactor name to an index.
     * @param nameToIndex map from name to state index
     */
    public void resolve(Map<String, Integer> nameToIndex) {
        if (interactorName != null && !interactorName.isEmpty()) {
            Integer idx = nameToIndex.get(interactorName);
            if (idx == null) throw new IllegalArgumentException("Unknown interactorName: " + interactorName);
            this.resolvedInteractorIndex = idx;
        } else {
            this.resolvedInteractorIndex = -1;
        }
    }

    @Override
    public double getFlowRate(double t, double fromValue, double[] state, Map<String, Double> params) {
        double interactor = (resolvedInteractorIndex >= 0 && resolvedInteractorIndex < state.length)
                ? state[resolvedInteractorIndex] : 1.0;
        return rate * fromValue * interactor;
    }

    @JsonProperty("rate")
    public double getRate() { return rate; }

    @JsonProperty("interactorName")
    public String getInteractorName() { return interactorName; }
}
