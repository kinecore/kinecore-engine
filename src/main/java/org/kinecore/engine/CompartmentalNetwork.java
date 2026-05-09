package org.kinecore.engine;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A bound compartmental ODE system.
 * 
 * <p>This class implements {@link FirstOrderDifferentialEquations} and is responsible for
 * calculating the rates of change (derivatives) for all compartments in the system.</p>
 */
public class CompartmentalNetwork implements FirstOrderDifferentialEquations {

    private final List<Compartment> compartments;
    private final List<IndexedFlux> fluxes;
    private final List<IndexedSourceSink> sources;
    private final List<FeedbackOperator> globalFeedbacks;
    private final List<AdvectionChain> advectionChains;
    private final Map<String, ExogenousSignal> exogenousSignals;
    private final Map<String, Double> params;
    private final boolean clampAtZero;

    public static class AdvectionChain {
        public final int[] indices;
        public boolean accumulateTerminal;
        public double shiftFrequency;
        public AdvectionChain(int[] indices, boolean acc, double freq) {
            this.indices = indices;
            this.accumulateTerminal = acc;
            this.shiftFrequency = freq;
        }
    }

    static class IndexedFlux {
        final int fromIdx;
        final int toIdx;
        final Flux flux;
        final List<FeedbackOperator> localFeedbacks;

        IndexedFlux(int fromIdx, int toIdx, Flux flux, List<FeedbackOperator> localFeedbacks) {
            this.fromIdx = fromIdx;
            this.toIdx   = toIdx;
            this.flux    = flux;
            this.localFeedbacks = localFeedbacks != null ? Collections.unmodifiableList(localFeedbacks) : Collections.emptyList();
        }
    }

    static class IndexedSourceSink {
        final int idx;
        final SourceSink sourceSink;
        final List<FeedbackOperator> localFeedbacks;

        IndexedSourceSink(int idx, SourceSink sourceSink, List<FeedbackOperator> localFeedbacks) {
            this.idx        = idx;
            this.sourceSink = sourceSink;
            this.localFeedbacks = localFeedbacks != null ? Collections.unmodifiableList(localFeedbacks) : Collections.emptyList();
        }
    }

    CompartmentalNetwork(List<Compartment>      compartments,
                         List<IndexedFlux>      fluxes,
                         List<IndexedSourceSink> sources,
                         List<FeedbackOperator> globalFeedbacks,
                         List<AdvectionChain>   advectionChains,
                         Map<String, ExogenousSignal> exogenousSignals,
                         Map<String, Double>    params,
                         boolean                clampAtZero) {
        this.compartments = Collections.unmodifiableList(compartments);
        this.fluxes       = Collections.unmodifiableList(fluxes);
        this.sources      = Collections.unmodifiableList(sources);
        this.globalFeedbacks = globalFeedbacks != null ? Collections.unmodifiableList(globalFeedbacks) : Collections.emptyList();
        this.advectionChains = advectionChains != null ? Collections.unmodifiableList(advectionChains) : Collections.emptyList();
        this.exogenousSignals = exogenousSignals != null ? Collections.unmodifiableMap(exogenousSignals) : Collections.emptyMap();
        this.params       = params != null ? Collections.unmodifiableMap(params) : Collections.emptyMap();
        this.clampAtZero  = clampAtZero;
    }

    /**
     * Creates a new instance of this network bound to the given parameters.
     * Crucial for thread-safe Monte Carlo simulations without ThreadLocal.
     * 
     * @param newParams the parameter map for this instance
     * @return a new bound network
     */
    public CompartmentalNetwork withParams(Map<String, Double> newParams) {
        return new CompartmentalNetwork(compartments, fluxes, sources, globalFeedbacks, advectionChains, exogenousSignals, newParams, clampAtZero);
    }

    @Override
    public int getDimension() {
        return compartments.size();
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        // 1. Reset derivatives
        for (int i = 0; i < yDot.length; i++) {
            yDot[i] = 0.0;
        }

        // 2. Apply fluxes
        for (IndexedFlux f : fluxes) {
            double flow = f.flux.getFlowRate(t, y[f.fromIdx], y, params);
            for (FeedbackOperator fb : f.localFeedbacks) {
                flow = fb.apply(t, flow, y, params);
            }
            for (FeedbackOperator fb : globalFeedbacks) {
                flow = fb.apply(t, flow, y, params);
            }
            if (flow < 0.0) flow = 0.0;

            Compartment source = compartments.get(f.fromIdx);
            if (clampAtZero && y[f.fromIdx] <= 1e-12) {
                flow = 0.0;
            } else if (y[f.fromIdx] <= source.getMin() + 1e-12) {
                flow = 0.0;
            }
            Compartment target = compartments.get(f.toIdx);
            if (y[f.toIdx] >= target.getMax() - 1e-12) {
                flow = 0.0;
            }

            yDot[f.fromIdx] -= flow;
            yDot[f.toIdx]   += flow;
        }

        // 3. Apply sources/sinks
        for (IndexedSourceSink ss : sources) {
            double net = ss.sourceSink.getNetFlow(t, y, params);
            for (FeedbackOperator fb : ss.localFeedbacks) {
                net = fb.apply(t, net, y, params);
            }
            for (FeedbackOperator fb : globalFeedbacks) {
                net = fb.apply(t, net, y, params);
            }

            Compartment c = compartments.get(ss.idx);
            if (net < 0 && clampAtZero && y[ss.idx] <= 1e-12) {
                net = 0.0;
            } else if (net < 0 && y[ss.idx] <= c.getMin() + 1e-12) {
                net = 0.0;
            } else if (net > 0 && y[ss.idx] >= c.getMax() - 1e-12) {
                net = 0.0;
            }

            yDot[ss.idx] += net;
        }

        // 4. Constraint Handling: Physical non-negativity and bounds
        for (int i = 0; i < yDot.length; i++) {
            Compartment c = compartments.get(i);
            if (c.isDerivativeLocked()) {
                yDot[i] = 0.0;
            }
        }
    }

    public List<AdvectionChain> getAdvectionChains() {
        return advectionChains;
    }
    
    public Map<String, ExogenousSignal> getExogenousSignals() {
        return exogenousSignals;
    }

    /** 
     * Gets the list of compartments.
     * @return the list of compartments in this network 
     */
    public List<Compartment> getCompartments() {
        return compartments;
    }

    /** 
     * Builds the initial state vector from the compartments.
     * @return a double array of initial values
     */
    public double[] buildInitialState() {
        return compartments.stream()
                           .mapToDouble(c -> c.getInitialValue(params))
                           .toArray();
    }
}
