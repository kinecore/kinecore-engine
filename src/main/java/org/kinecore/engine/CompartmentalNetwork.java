package org.kinecore.engine;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * A bound compartmental ODE system.
 * 
 * <p>Implements the ODE derivative function. Optimized for performance by using a 
 * pre-allocated workspace array for aggregates to avoid the "Allocation Storm".</p>
 */
public class CompartmentalNetwork implements FirstOrderDifferentialEquations {

    private final List<Compartment> compartments;
    private final List<IndexedFlux> fluxes;
    private final List<IndexedSourceSink> sources;
    private final List<FeedbackOperator> globalFeedbacks;
    private final List<AdvectionChain> advectionChains;
    private final Map<String, ExogenousSignal> exogenousSignals;
    private final List<Aggregate> aggregates;
    private final Map<String, Double> params;
    private final boolean clampAtZero;
    
    /** Pre-allocated workspace to avoid GC pressure in the ODE loop */
    private final double[] workspaceState;

    public static class Aggregate {
        public final String name;
        public final int[] sourceIndices;

        public Aggregate(String name, int[] sourceIndices) {
            this.name = name;
            this.sourceIndices = sourceIndices;
        }

        public double calculate(double[] y) {
            double sum = 0;
            for (int idx : sourceIndices) {
                sum += y[idx];
            }
            return sum;
        }
    }

    /**
     * Represents a discrete "aging" chain where mass shifts between cohorts at 
     * a specific frequency. Uses name-based definition for robustness (Upgrade 3).
     */
    public static class AdvectionChain {
        private final String[] sourceNames;
        public boolean accumulateTerminal;
        public double shiftFrequency;
        
        /** Resolved indices, used during simulation for performance */
        private transient int[] resolvedIndices;

        @com.fasterxml.jackson.annotation.JsonCreator
        public AdvectionChain(@com.fasterxml.jackson.annotation.JsonProperty("sourceNames") String[] names, 
                              @com.fasterxml.jackson.annotation.JsonProperty("accumulateTerminal") boolean acc, 
                              @com.fasterxml.jackson.annotation.JsonProperty("shiftFrequency") double freq) {
            this.sourceNames = names;
            this.accumulateTerminal = acc;
            this.shiftFrequency = freq;
        }
        
        /**
         * Resolves compartment names to indices.
         * @param nameToIndex map from name to state index
         */
        public void resolve(Map<String, Integer> nameToIndex) {
            this.resolvedIndices = new int[sourceNames.length];
            for (int i = 0; i < sourceNames.length; i++) {
                Integer idx = nameToIndex.get(sourceNames[i]);
                if (idx == null) throw new IllegalArgumentException("Unknown sourceName in AdvectionChain: " + sourceNames[i]);
                this.resolvedIndices[i] = idx;
            }
        }

        public int[] getIndices() {
            return resolvedIndices;
        }
        
        @com.fasterxml.jackson.annotation.JsonProperty("sourceNames")
        public String[] getSourceNames() {
            return sourceNames;
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
                         List<Aggregate>        aggregates,
                         Map<String, Double>    params,
                         boolean                clampAtZero) {
        this.compartments = Collections.unmodifiableList(compartments);
        this.fluxes       = Collections.unmodifiableList(fluxes);
        this.sources      = Collections.unmodifiableList(sources);
        this.globalFeedbacks = globalFeedbacks != null ? Collections.unmodifiableList(globalFeedbacks) : Collections.emptyList();
        this.advectionChains = advectionChains != null ? Collections.unmodifiableList(advectionChains) : Collections.emptyList();
        this.exogenousSignals = exogenousSignals != null ? Collections.unmodifiableMap(exogenousSignals) : Collections.emptyMap();
        this.aggregates   = aggregates != null ? Collections.unmodifiableList(aggregates) : Collections.emptyList();
        this.params       = params != null ? Collections.unmodifiableMap(params) : Collections.emptyMap();
        this.clampAtZero  = clampAtZero;
        
        // Pre-allocate workspace
        this.workspaceState = new double[compartments.size() + this.aggregates.size()];
    }

    public CompartmentalNetwork withParams(Map<String, Double> newParams) {
        return new CompartmentalNetwork(compartments, fluxes, sources, globalFeedbacks, advectionChains, exogenousSignals, aggregates, newParams, clampAtZero);
    }

    @Override
    public int getDimension() {
        return compartments.size();
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        System.arraycopy(y, 0, workspaceState, 0, y.length);
        for (int i = 0; i < aggregates.size(); i++) {
            workspaceState[y.length + i] = aggregates.get(i).calculate(y);
        }

        for (int i = 0; i < yDot.length; i++) {
            yDot[i] = 0.0;
        }

        for (IndexedFlux f : fluxes) {
            double flow = f.flux.getFlowRate(t, y[f.fromIdx], workspaceState, params);
            for (FeedbackOperator fb : f.localFeedbacks) {
                flow = fb.apply(t, flow, workspaceState, params);
            }
            for (FeedbackOperator fb : globalFeedbacks) {
                flow = fb.apply(t, flow, workspaceState, params);
            }
            if (flow < 0.0) flow = 0.0;

            Compartment source = compartments.get(f.fromIdx);
            double sourceMin = clampAtZero ? Math.max(0.0, source.getMin()) : source.getMin();
            flow = applySoftBoundary(flow, y[f.fromIdx], sourceMin, false); 

            Compartment target = compartments.get(f.toIdx);
            flow = applySoftBoundary(flow, y[f.toIdx], target.getMax(), true);

            yDot[f.fromIdx] -= flow;
            yDot[f.toIdx]   += flow;
        }

        for (IndexedSourceSink ss : sources) {
            double net = ss.sourceSink.getNetFlow(t, workspaceState, params);
            for (FeedbackOperator fb : ss.localFeedbacks) {
                net = fb.apply(t, net, workspaceState, params);
            }
            for (FeedbackOperator fb : globalFeedbacks) {
                net = fb.apply(t, net, workspaceState, params);
            }

            Compartment c = compartments.get(ss.idx);
            double cMin = clampAtZero ? Math.max(0.0, c.getMin()) : c.getMin();
            
            if (net > 0) {
                net = applySoftBoundary(net, y[ss.idx], c.getMax(), true);
            } else if (net < 0) {
                net = -applySoftBoundary(-net, y[ss.idx], cMin, false);
            }

            yDot[ss.idx] += net;
        }

        for (int i = 0; i < yDot.length; i++) {
            Compartment c = compartments.get(i);
            if (c.isDerivativeLocked()) {
                yDot[i] = 0.0;
            }
        }
    }

    private double applySoftBoundary(double flow, double current, double boundary, boolean isMax) {
        if (isMax && boundary == Double.POSITIVE_INFINITY) return flow;
        if (!isMax && boundary == Double.NEGATIVE_INFINITY) return flow;

        double distance = isMax ? (boundary - current) : (current - boundary);
        if (distance < org.kinecore.util.MathUtils.NEAR_ZERO) return 0.0;
        if (distance > 1.0) return flow;
        
        double damping = distance / (distance + org.kinecore.util.MathUtils.DAMPING_EPSILON);
        return flow * damping;
    }

    public double[] buildInitialState() {
        double[] y = new double[compartments.size()];
        for (int i = 0; i < compartments.size(); i++) {
            y[i] = compartments.get(i).getInitialValue(params);
        }
        return y;
    }

    public List<AdvectionChain> getAdvectionChains() {
        return advectionChains;
    }
    
    public Map<String, ExogenousSignal> getExogenousSignals() {
        return exogenousSignals;
    }

    public List<Compartment> getCompartments() {
        return compartments;
    }
}
