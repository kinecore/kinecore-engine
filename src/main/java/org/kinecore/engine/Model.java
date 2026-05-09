package org.kinecore.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

/**
 * A concrete, serializable representation of a System Dynamics model.
 * 
 * <p>Unlike a functional lambda, this class stores the full model structure
 * (compartments, fluxes, advection chains) as data, allowing it to be
 * round-tripped through the REST API for the Next.js dashboard.</p>
 */
public class Model implements ModelDefinition {

    private final List<Compartment> compartments;
    private final List<IndexedFluxDef> fluxes;
    private final List<IndexedSourceSinkDef> sources;
    private final List<FeedbackOperator> globalFeedbacks;
    private final List<CompartmentalNetwork.AdvectionChain> advectionChains;
    private final Map<String, ExogenousSignal> exogenousSignals;
    private final boolean clampAtZero;

    @JsonCreator
    public Model(@JsonProperty("compartments")     List<Compartment> compartments,
                 @JsonProperty("fluxes")           List<IndexedFluxDef> fluxes,
                 @JsonProperty("sources")          List<IndexedSourceSinkDef> sources,
                 @JsonProperty("globalFeedbacks")  List<FeedbackOperator> globalFeedbacks,
                 @JsonProperty("advectionChains")  List<CompartmentalNetwork.AdvectionChain> advectionChains,
                 @JsonProperty("exogenousSignals") Map<String, ExogenousSignal> exogenousSignals,
                 @JsonProperty("clampAtZero")      boolean clampAtZero) {
        this.compartments = compartments != null ? compartments : new ArrayList<>();
        this.fluxes = fluxes != null ? fluxes : new ArrayList<>();
        this.sources = sources != null ? sources : new ArrayList<>();
        this.globalFeedbacks = globalFeedbacks != null ? globalFeedbacks : new ArrayList<>();
        this.advectionChains = advectionChains != null ? advectionChains : new ArrayList<>();
        this.exogenousSignals = exogenousSignals != null ? exogenousSignals : new HashMap<>();
        this.clampAtZero = clampAtZero;
    }

    @Override
    public CompartmentalNetwork bind(Map<String, Double> params) {
        List<CompartmentalNetwork.IndexedFlux> boundFluxes = new ArrayList<>();
        for (IndexedFluxDef f : fluxes) {
            boundFluxes.add(new CompartmentalNetwork.IndexedFlux(f.from, f.to, f.logic, f.feedbacks));
        }

        List<CompartmentalNetwork.IndexedSourceSink> boundSources = new ArrayList<>();
        for (IndexedSourceSinkDef s : sources) {
            boundSources.add(new CompartmentalNetwork.IndexedSourceSink(s.index, s.logic, s.feedbacks));
        }

        return new CompartmentalNetwork(
                compartments,
                boundFluxes,
                boundSources,
                globalFeedbacks,
                advectionChains,
                exogenousSignals,
                params,
                clampAtZero
        );
    }

    @JsonProperty("compartments")
    public List<Compartment> getCompartments() { return compartments; }

    @JsonProperty("fluxes")
    public List<IndexedFluxDef> getFluxes() { return fluxes; }

    @JsonProperty("sources")
    public List<IndexedSourceSinkDef> getSources() { return sources; }

    @JsonProperty("globalFeedbacks")
    public List<FeedbackOperator> getGlobalFeedbacks() { return globalFeedbacks; }

    @JsonProperty("advectionChains")
    public List<CompartmentalNetwork.AdvectionChain> getAdvectionChains() { return advectionChains; }

    @JsonProperty("exogenousSignals")
    public Map<String, ExogenousSignal> getExogenousSignals() { return exogenousSignals; }

    @JsonProperty("clampAtZero")
    public boolean isClampAtZero() { return clampAtZero; }

    /** Helper DTO for serializing internal flux state */
    public static class IndexedFluxDef {
        public int from, to;
        public Flux logic;
        public List<FeedbackOperator> feedbacks;

        @JsonCreator
        public IndexedFluxDef(@JsonProperty("from") int from, 
                             @JsonProperty("to") int to, 
                             @JsonProperty("logic") Flux logic, 
                             @JsonProperty("feedbacks") List<FeedbackOperator> feedbacks) {
            this.from = from;
            this.to = to;
            this.logic = logic;
            this.feedbacks = feedbacks != null ? feedbacks : new ArrayList<>();
        }
    }

    /** Helper DTO for serializing internal source/sink state */
    public static class IndexedSourceSinkDef {
        public int index;
        public SourceSink logic;
        public List<FeedbackOperator> feedbacks;

        @JsonCreator
        public IndexedSourceSinkDef(@JsonProperty("index") int index, 
                                   @JsonProperty("logic") SourceSink logic, 
                                   @JsonProperty("feedbacks") List<FeedbackOperator> feedbacks) {
            this.index = index;
            this.logic = logic;
            this.feedbacks = feedbacks != null ? feedbacks : new ArrayList<>();
        }
    }
}
