package org.kinecore.engine;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Enhanced Fluent DSL for constructing System Dynamics models.
 * 
 * <p>Provides high-level abstractions for age-structured and compartmental models,
 * including series creation, linear chain fluxes, and non-linear stress responses.</p>
 */
public class CompartmentalNetworkBuilder {

    private final List<Compartment>                       compartments = new ArrayList<>();
    private final List<Compartment>                       lastAddedCompartments = new ArrayList<>();
    private final List<CompartmentalNetwork.IndexedFlux>      fluxes   = new ArrayList<>();
    private final List<CompartmentalNetwork.IndexedSourceSink> sources  = new ArrayList<>();
    private final List<FeedbackOperator>                  globalFeedbacks = new ArrayList<>();
    private final Map<String, FeedbackOperator>           namedFeedbacks  = new HashMap<>();
    private final Map<String, int[]>                      aggregates      = new HashMap<>();
    private final Map<String, ExogenousSignal>            exogenousSignals = new HashMap<>();
    private final List<CompartmentalNetwork.AdvectionChain> advectionChains = new ArrayList<>();
    private boolean                                       clampAtZero     = false;

    public class AdvectionChainConfigurator {
        private final CompartmentalNetwork.AdvectionChain chain;
        AdvectionChainConfigurator(CompartmentalNetwork.AdvectionChain chain) { this.chain = chain; }
        public AdvectionChainConfigurator accumulateTerminal(boolean acc) { chain.accumulateTerminal = acc; return this; }
        public AdvectionChainConfigurator shiftFrequency(double freq) { chain.shiftFrequency = freq; return this; }
    }

    public CompartmentalNetworkBuilder() {}

    /**
     * Adds a series of numbered compartments (e.g., Age_0 to Age_100).
     * @param prefix name prefix
     * @param start start index
     * @param end end index
     * @param initialValueMapper mapper from index to initial value
     * @return this builder
     */
    public CompartmentalNetworkBuilder addCompartmentSeries(String prefix, int start, int end, Function<Integer, Double> initialValueMapper) {
        lastAddedCompartments.clear();
        for (int i = start; i <= end; i++) {
            Compartment c = new Compartment(prefix + "_" + i, compartments.size(), initialValueMapper.apply(i));
            compartments.add(c);
            lastAddedCompartments.add(c);
        }
        return this;
    }

    public CompartmentalNetworkBuilder addCompartment(String name, double initialValue) {
        lastAddedCompartments.clear();
        Compartment c = new Compartment(name, compartments.size(), initialValue);
        compartments.add(c);
        lastAddedCompartments.add(c);
        return this;
    }

    public CompartmentalNetworkBuilder withMin(double min) {
        for (Compartment c : lastAddedCompartments) c.withMin(min);
        return this;
    }

    public CompartmentalNetworkBuilder withMax(double max) {
        for (Compartment c : lastAddedCompartments) c.withMax(max);
        return this;
    }

    public CompartmentalNetworkBuilder lockDerivative(boolean locked) {
        for (Compartment c : lastAddedCompartments) c.lockDerivative(locked);
        return this;
    }

    public AdvectionChainConfigurator addAdvectionChain(String prefix, int start, int end) {
        int[] indices = IntStream.rangeClosed(start, end).map(i -> requireCompartmentIndex(prefix + "_" + i)).toArray();
        CompartmentalNetwork.AdvectionChain chain = new CompartmentalNetwork.AdvectionChain(indices, false, 1.0);
        advectionChains.add(chain);
        return new AdvectionChainConfigurator(chain);
    }

    public CompartmentalNetworkBuilder addExogenousVariable(String name, ExogenousSignal signal) {
        exogenousSignals.put(name, signal);
        return this;
    }

    /**
     * Defines a named aggregate (e.g., "Workers" = Age_15 to Age_64).
     * @param aggregateName name of the aggregate
     * @param prefix compartment name prefix
     * @param start start index
     * @param end end index
     * @return this builder
     */
    public CompartmentalNetworkBuilder defineAggregate(String aggregateName, String prefix, int start, int end) {
        int[] indices = IntStream.rangeClosed(start, end)
                .map(i -> requireCompartmentIndex(prefix + "_" + i))
                .toArray();
        aggregates.put(aggregateName, indices);
        return this;
    }

    /**
     * Helper to sum an aggregate from the state vector.
     * @param state the system state vector
     * @param indices indices to sum
     * @return the sum
     */
    public static double sumAggregate(double[] state, int[] indices) {
        double sum = 0;
        for (int idx : indices) sum += state[idx];
        return sum;
    }

    /**
     * Automatically creates fluxes between sequential compartments in a series (e.g., Aging).
     * @param prefix compartment name prefix
     * @param start start index
     * @param end end index
     * @param fluxLogic the logic for the flow
     * @param activeFeedbacks names of selective feedbacks to apply
     * @return this builder
     */
    public CompartmentalNetworkBuilder addChainFlux(String prefix, int start, int end, Flux fluxLogic, String... activeFeedbacks) {
        for (int i = start; i < end; i++) {
            addFlux(prefix + "_" + i, prefix + "_" + (i + 1), fluxLogic, activeFeedbacks);
        }
        return this;
    }

    /**
     * High-level Stress Response: Multiplies target fluxes by an exponential decay 
     * based on an aggregate ratio (e.g., EDR).
     * 
     * @param name reference name for this feedback
     * @param aggregateNum name of numerator aggregate
     * @param aggregateDen name of denominator aggregate
     * @param threshold activation threshold (ReLU-style)
     * @param sensitivity exponential decay sensitivity
     * @return this builder
     */
    public CompartmentalNetworkBuilder addStressResponse(String name, String aggregateNum, String aggregateDen, double threshold, double sensitivity) {
        int[] numIdx = aggregates.get(aggregateNum);
        int[] denIdx = aggregates.get(aggregateDen);
        
        if (numIdx == null || denIdx == null) {
            throw new IllegalArgumentException("Aggregate not found: " + (numIdx == null ? aggregateNum : aggregateDen));
        }

        FeedbackOperator stressOp = (t, flow, state, params) -> {
            double n = 0; for(int i : numIdx) n += state[i];
            double d = 0; for(int i : denIdx) d += state[i];
            double ratio = (d > 0) ? (n / d) : 0;
            double stress = Math.max(0, ratio - threshold);
            return flow * Math.exp(-sensitivity * stress);
        };
        
        return addFeedback(name, stressOp);
    }

    /**
     * Registers a named feedback operator.
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
     * Adds a flux between compartments.
     * @param fromName source
     * @param toName target
     * @param flux logic
     * @param activeFeedbacks names of selective feedbacks to apply
     * @return this builder
     */
    public CompartmentalNetworkBuilder addFlux(String fromName, String toName, Flux flux, String... activeFeedbacks) {
        int fromIdx = requireCompartmentIndex(fromName);
        int toIdx = requireCompartmentIndex(toName);
        List<FeedbackOperator> localOps = resolveFeedbacks(activeFeedbacks);
        fluxes.add(new CompartmentalNetwork.IndexedFlux(fromIdx, toIdx, flux, localOps));
        return this;
    }

    /**
     * Convenience for constant rate flux.
     * @param fromName source
     * @param toName target
     * @param rate fixed rate
     * @param activeFeedbacks names of selective feedbacks to apply
     * @return this builder
     */
    public CompartmentalNetworkBuilder addConstantRateFlux(String fromName, String toName, double rate, String... activeFeedbacks) {
        if (rate < 0) throw new IllegalArgumentException("Constant flux rate must be >= 0");
        return addFlux(fromName, toName, (t, fromVal, state, params) -> rate * fromVal, activeFeedbacks);
    }

    /**
     * Adds an external source or sink.
     * @param compartmentName target
     * @param sourceSink logic
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
     * Enables numerical clamping at zero.
     * @param enabled true to enable
     * @return this builder
     */
    public CompartmentalNetworkBuilder clampAtZero(boolean enabled) {
        this.clampAtZero = enabled;
        return this;
    }

    /**
     * Finalizes the model definition.
     * @return the stateless model definition
     */
    public ModelDefinition buildDefinition() {
        if (compartments.isEmpty()) throw new IllegalStateException("No compartments defined.");
        List<Compartment> compsSnapshot = new ArrayList<>(compartments);
        List<CompartmentalNetwork.IndexedFlux> fluxesSnapshot = new ArrayList<>(fluxes);
        List<CompartmentalNetwork.IndexedSourceSink> sourcesSnapshot = new ArrayList<>(sources);
        List<FeedbackOperator> globalsSnapshot = new ArrayList<>(globalFeedbacks);
        List<CompartmentalNetwork.AdvectionChain> chainsSnapshot = new ArrayList<>(advectionChains);
        Map<String, ExogenousSignal> signalsSnapshot = new HashMap<>(exogenousSignals);
        boolean clamp = this.clampAtZero;

        return params -> new CompartmentalNetwork(
                compsSnapshot,
                fluxesSnapshot,
                sourcesSnapshot,
                globalsSnapshot,
                chainsSnapshot,
                signalsSnapshot,
                params,
                clamp
        );
    }

    private int requireCompartmentIndex(String name) {
        for (int i = 0; i < compartments.size(); i++) {
            if (compartments.get(i).getName().equals(name)) return i;
        }
        throw new IllegalArgumentException("Compartment not found: " + name);
    }

    private List<FeedbackOperator> resolveFeedbacks(String[] names) {
        List<FeedbackOperator> ops = new ArrayList<>();
        if (names != null) {
            for (String n : names) {
                FeedbackOperator op = namedFeedbacks.get(n);
                if (op == null) throw new IllegalArgumentException("Feedback not found: " + n);
                ops.add(op);
            }
        }
        return ops;
    }
}
