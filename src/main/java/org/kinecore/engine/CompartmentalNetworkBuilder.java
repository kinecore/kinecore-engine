package org.kinecore.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Fluent DSL for constructing a {@link ModelDefinition} declaratively.
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * ModelDefinition model = new CompartmentalNetworkBuilder()
 *     .addCompartment("S", 1000)
 *     .addFlux("S", "I", fluxLambda)
 *     .clampAtZero(true)
 *     .buildDefinition();
 * }</pre>
 */
public class CompartmentalNetworkBuilder {

    private final List<Compartment>                       compartments = new ArrayList<>();
    private final List<CompartmentalNetwork.IndexedFlux>      fluxes   = new ArrayList<>();
    private final List<CompartmentalNetwork.IndexedSourceSink> sources  = new ArrayList<>();
    private final List<FeedbackOperator>                  globalFeedbacks = new ArrayList<>();
    private final Map<String, FeedbackOperator>           namedFeedbacks  = new HashMap<>();
    private boolean                                       clampAtZero     = false;

    /**
     * Constructor for the builder.
     */
    public CompartmentalNetworkBuilder() {}

    /**
     * Adds a compartment with a static initial value.
     * @param name unique name
     * @param initialValue starting value
     * @return this builder
     */
    public CompartmentalNetworkBuilder addCompartment(String name, double initialValue) {
        compartments.add(new Compartment(name, compartments.size(), initialValue));
        return this;
    }

    /**
     * Adds a compartment with a parameterized initial value.
     * @param name unique name
     * @param initialValueSupplier function of parameters
     * @return this builder
     */
    public CompartmentalNetworkBuilder addCompartment(String name, Function<Map<String, Double>, Double> initialValueSupplier) {
        compartments.add(new Compartment(name, compartments.size(), initialValueSupplier));
        return this;
    }

    /**
     * Registers a named feedback operator for selective use.
     * @param name reference name
     * @param feedback the operator
     * @return this builder
     */
    public CompartmentalNetworkBuilder addFeedback(String name, FeedbackOperator feedback) {
        namedFeedbacks.put(name, feedback);
        return this;
    }

    /**
     * Adds a global feedback operator applied to ALL fluxes and sources.
     * @param feedback the operator
     * @return this builder
     */
    public CompartmentalNetworkBuilder addGlobalFeedback(FeedbackOperator feedback) {
        globalFeedbacks.add(feedback);
        return this;
    }

    /**
     * Enables numerical clamping at zero to prevent negative values in physical systems.
     * @param enabled true to enable clamping
     * @return this builder
     */
    public CompartmentalNetworkBuilder clampAtZero(boolean enabled) {
        this.clampAtZero = enabled;
        return this;
    }

    /**
     * Adds a flux between compartments.
     * @param fromName source
     * @param toName target
     * @param flux logic
     * @param activeFeedbacks names of selective feedbacks to apply
     * @return this builder
     */
    public CompartmentalNetworkBuilder addFlux(String fromName, String toName, Flux flux, String... activeFeedbacks) {
        int fromIdx = requireCompartmentIndex(fromName);
        int toIdx   = requireCompartmentIndex(toName);
        List<FeedbackOperator> localOps = resolveFeedbacks(activeFeedbacks);
        fluxes.add(new CompartmentalNetwork.IndexedFlux(fromIdx, toIdx, flux, localOps));
        return this;
    }

    /**
     * Convenience for constant rate flux.
     * @param fromName source compartment name
     * @param toName target compartment name
     * @param rate fixed rate of flow
     * @param activeFeedbacks names of selective feedbacks to apply
     * @return this builder
     */
    public CompartmentalNetworkBuilder addConstantRateFlux(String fromName, String toName, double rate, String... activeFeedbacks) {
        if (rate < 0) throw new IllegalArgumentException("Constant flux rate must be >= 0, got: " + rate);
        return addFlux(fromName, toName, (t, fromVal, state, params) -> rate * fromVal, activeFeedbacks);
    }

    /**
     * Adds an external source or sink.
     * @param compartmentName target compartment name
     * @param sourceSink logic for the source/sink
     * @param activeFeedbacks names of selective feedbacks to apply
     * @return this builder
     */
    public CompartmentalNetworkBuilder addSource(String compartmentName, SourceSink sourceSink, String... activeFeedbacks) {
        int idx = requireCompartmentIndex(compartmentName);
        List<FeedbackOperator> localOps = resolveFeedbacks(activeFeedbacks);
        sources.add(new CompartmentalNetwork.IndexedSourceSink(idx, sourceSink, localOps));
        return this;
    }

    /**
     * Finalizes the model definition.
     * @return the stateless model definition
     */
    public ModelDefinition buildDefinition() {
        if (compartments.isEmpty()) {
            throw new IllegalStateException("Cannot build a model with zero compartments.");
        }
        List<Compartment> compsSnapshot = new ArrayList<>(compartments);
        List<CompartmentalNetwork.IndexedFlux> fluxesSnapshot = new ArrayList<>(fluxes);
        List<CompartmentalNetwork.IndexedSourceSink> sourcesSnapshot = new ArrayList<>(sources);
        List<FeedbackOperator> globalsSnapshot = new ArrayList<>(globalFeedbacks);
        boolean clamp = this.clampAtZero;

        return params -> new CompartmentalNetwork(
                compsSnapshot,
                fluxesSnapshot,
                sourcesSnapshot,
                globalsSnapshot,
                params,
                clamp
        );
    }

    private int requireCompartmentIndex(String name) {
        for (int i = 0; i < compartments.size(); i++) {
            if (compartments.get(i).getName().equals(name)) return i;
        }
        throw new IllegalArgumentException("Compartment not found: '" + name + "'");
    }
    
    private List<FeedbackOperator> resolveFeedbacks(String[] names) {
        List<FeedbackOperator> ops = new ArrayList<>();
        if (names != null) {
            for (String fbName : names) {
                FeedbackOperator op = namedFeedbacks.get(fbName);
                if (op == null) throw new IllegalArgumentException("Feedback not found: " + fbName);
                ops.add(op);
            }
        }
        return ops;
    }
}
