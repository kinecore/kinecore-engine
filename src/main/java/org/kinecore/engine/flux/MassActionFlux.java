package org.kinecore.engine.flux;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.Flux;

import java.util.Map;

/**
 * A flux driven by mass-action kinetics: flow = rate × fromValue × state[interactorIndex].
 *
 * <p>Useful for modelling contagion, predator–prey, or bimolecular reactions where
 * the transfer rate is proportional to both the source and an interacting compartment.</p>
 *
 * <p>JSON example:
 * <pre>{ "type": "massAction", "rate": 0.3, "interactorIndex": 2 }</pre>
 * </p>
 */
public class MassActionFlux implements Flux {

    /** Reaction rate constant. */
    private final double rate;

    /**
     * Index of the interactor compartment in the state vector.
     * Set to -1 to use only the source value (degrades to first-order kinetics).
     */
    private final int interactorIndex;

    /** No-arg constructor for Jackson. */
    public MassActionFlux() { this.rate = 0.0; this.interactorIndex = -1; }

    /**
     * Constructs a MassActionFlux.
     * @param rate            rate constant
     * @param interactorIndex state-vector index of the interacting compartment
     */
    @JsonCreator
    public MassActionFlux(@JsonProperty("rate") double rate,
                          @JsonProperty("interactorIndex") int interactorIndex) {
        this.rate             = rate;
        this.interactorIndex  = interactorIndex;
    }

    /**
     * Gets the reaction rate constant.
     * @return rate constant
     */
    public double getRate()           { return rate; }

    /**
     * Gets the index of the interacting compartment in the state vector.
     * @return interactor compartment index, or -1 for first-order kinetics
     */
    public int getInteractorIndex()   { return interactorIndex; }

    @Override
    public double getFlowRate(double t, double fromValue, double[] state, Map<String, Double> params) {
        double interactor = (interactorIndex >= 0 && interactorIndex < state.length)
                ? state[interactorIndex] : 1.0;
        return rate * fromValue * interactor;
    }
}
