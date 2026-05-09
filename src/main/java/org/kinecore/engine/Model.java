package org.kinecore.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.kinecore.engine.feedback.LogisticFeedback;
import org.kinecore.engine.flux.MassActionFlux;
import org.kinecore.engine.source.AggregatedSource;

import java.util.*;

/**
 * A concrete, serializable representation of a System Dynamics model.
 * 
 * <p>Includes pre-flight validation and named aggregates to ensure the engine is
 * robust against user error and complex model definitions.</p>
 */
public class Model implements ModelDefinition {

    private final List<Compartment> compartments;
    private final List<IndexedFluxDef> fluxes;
    private final List<IndexedSourceSinkDef> sources;
    private final List<FeedbackOperator> globalFeedbacks;
    private final List<CompartmentalNetwork.AdvectionChain> advectionChains;
    private final Map<String, ExogenousSignal> exogenousSignals;
    private final List<AggregateDef> aggregates;
    private final boolean clampAtZero;

    @JsonCreator
    public Model(@JsonProperty("compartments")     List<Compartment> compartments,
                 @JsonProperty("fluxes")           List<IndexedFluxDef> fluxes,
                 @JsonProperty("sources")          List<IndexedSourceSinkDef> sources,
                 @JsonProperty("globalFeedbacks")  List<FeedbackOperator> globalFeedbacks,
                 @JsonProperty("advectionChains")  List<CompartmentalNetwork.AdvectionChain> advectionChains,
                 @JsonProperty("exogenousSignals") Map<String, ExogenousSignal> exogenousSignals,
                 @JsonProperty("aggregates")       List<AggregateDef> aggregates,
                 @JsonProperty("clampAtZero")      boolean clampAtZero) {
        this.compartments = compartments != null ? compartments : new ArrayList<>();
        this.fluxes = fluxes != null ? fluxes : new ArrayList<>();
        this.sources = sources != null ? sources : new ArrayList<>();
        this.globalFeedbacks = globalFeedbacks != null ? globalFeedbacks : new ArrayList<>();
        this.advectionChains = advectionChains != null ? advectionChains : new ArrayList<>();
        this.exogenousSignals = exogenousSignals != null ? exogenousSignals : new HashMap<>();
        this.aggregates = aggregates != null ? aggregates : new ArrayList<>();
        this.clampAtZero = clampAtZero;
    }

    /**
     * Performs a "Deep Pre-Flight" validation (v1.1.6) to ensure all names, 
     * aggregates, and internal logic components are correctly wired.
     * 
     * @return list of error messages, or empty list if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        Set<String> validNames = new HashSet<>();
        for (Compartment c : compartments) validNames.add(c.getName());
        for (AggregateDef a : aggregates) validNames.add(a.name);

        for (IndexedFluxDef f : fluxes) {
            if (!validNames.contains(f.fromName)) errors.add("Flux fromName not found: " + f.fromName);
            if (!validNames.contains(f.toName)) errors.add("Flux toName not found: " + f.toName);
            deepValidate(f.logic, validNames, errors);
            for (FeedbackOperator fb : f.feedbacks) deepValidate(fb, validNames, errors);
        }
        for (IndexedSourceSinkDef s : sources) {
            if (!validNames.contains(s.targetName)) errors.add("Source targetName not found: " + s.targetName);
            deepValidate(s.logic, validNames, errors);
            for (FeedbackOperator fb : s.feedbacks) deepValidate(fb, validNames, errors);
        }
        for (FeedbackOperator fb : globalFeedbacks) deepValidate(fb, validNames, errors);
        
        for (CompartmentalNetwork.AdvectionChain chain : advectionChains) {
            for (String name : chain.getSourceNames()) {
                if (!validNames.contains(name)) errors.add("AdvectionChain source not found: " + name);
            }
        }
        
        return errors;
    }
    
    private void deepValidate(Object logic, Set<String> validNames, List<String> errors) {
        if (logic instanceof LogisticFeedback) {
            String name = ((LogisticFeedback) logic).getStateName();
            if (!validNames.contains(name)) errors.add("LogisticFeedback dependency not found: " + name);
        } else if (logic instanceof AggregatedSource) {
            for (String name : ((AggregatedSource) logic).getSourceNames()) {
                if (!validNames.contains(name)) errors.add("AggregatedSource dependency not found: " + name);
            }
        } else if (logic instanceof MassActionFlux) {
            String name = ((MassActionFlux) logic).getInteractorName();
            if (name != null && !validNames.contains(name)) errors.add("MassActionFlux interactor not found: " + name);
        }
    }

    @Override
    public CompartmentalNetwork bind(Map<String, Double> params) {
        // Deep validate before binding (The Shield)
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Model validation failed: " + String.join("; ", errors));
        }

        Map<String, Integer> nameToIndex = new HashMap<>();
        for (int i = 0; i < compartments.size(); i++) {
            nameToIndex.put(compartments.get(i).getName(), i);
        }
        
        for (int i = 0; i < aggregates.size(); i++) {
            nameToIndex.put(aggregates.get(i).name, compartments.size() + i);
        }

        List<CompartmentalNetwork.IndexedFlux> boundFluxes = new ArrayList<>();
        for (IndexedFluxDef f : fluxes) {
            int fromIdx = nameToIndex.get(f.fromName);
            int toIdx = nameToIndex.get(f.toName);
            
            resolveLogic(f.logic, nameToIndex);
            for (FeedbackOperator fb : f.feedbacks) resolveLogic(fb, nameToIndex);
            
            boundFluxes.add(new CompartmentalNetwork.IndexedFlux(fromIdx, toIdx, f.logic, f.feedbacks));
        }

        List<CompartmentalNetwork.IndexedSourceSink> boundSources = new ArrayList<>();
        for (IndexedSourceSinkDef s : sources) {
            int targetIdx = nameToIndex.get(s.targetName);
            
            resolveLogic(s.logic, nameToIndex);
            for (FeedbackOperator fb : s.feedbacks) resolveLogic(fb, nameToIndex);
            
            boundSources.add(new CompartmentalNetwork.IndexedSourceSink(targetIdx, s.logic, s.feedbacks));
        }
        
        for (FeedbackOperator fb : globalFeedbacks) resolveLogic(fb, nameToIndex);
        
        List<CompartmentalNetwork.Aggregate> boundAggregates = new ArrayList<>();
        for (AggregateDef ad : aggregates) {
            int[] indices = new int[ad.sourceNames.length];
            for (int i = 0; i < ad.sourceNames.length; i++) {
                indices[i] = nameToIndex.get(ad.sourceNames[i]);
            }
            boundAggregates.add(new CompartmentalNetwork.Aggregate(ad.name, indices));
        }

        for (CompartmentalNetwork.AdvectionChain chain : advectionChains) {
            chain.resolve(nameToIndex);
        }

        return new CompartmentalNetwork(
                compartments,
                boundFluxes,
                boundSources,
                globalFeedbacks,
                advectionChains,
                exogenousSignals,
                boundAggregates,
                params,
                clampAtZero
        );
    }
    
    private void resolveLogic(Object logic, Map<String, Integer> nameToIndex) {
        if (logic instanceof LogisticFeedback) {
            ((LogisticFeedback) logic).resolve(nameToIndex);
        } else if (logic instanceof AggregatedSource) {
            ((AggregatedSource) logic).resolve(nameToIndex);
        } else if (logic instanceof MassActionFlux) {
            ((MassActionFlux) logic).resolve(nameToIndex);
        }
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

    @JsonProperty("aggregates")
    public List<AggregateDef> getAggregates() { return aggregates; }

    @JsonProperty("clampAtZero")
    public boolean isClampAtZero() { return clampAtZero; }

    public static class AggregateDef {
        public String name;
        public String[] sourceNames;
        
        @JsonCreator
        public AggregateDef(@JsonProperty("name") String name, 
                            @JsonProperty("sourceNames") String[] sourceNames) {
            this.name = name;
            this.sourceNames = sourceNames;
        }
    }

    public static class IndexedFluxDef {
        public String fromName, toName;
        public Flux logic;
        public List<FeedbackOperator> feedbacks;

        @JsonCreator
        public IndexedFluxDef(@JsonProperty("fromName") String fromName, 
                             @JsonProperty("toName") String toName, 
                             @JsonProperty("logic") Flux logic, 
                             @JsonProperty("feedbacks") List<FeedbackOperator> feedbacks) {
            this.fromName = fromName;
            this.toName = toName;
            this.logic = logic;
            this.feedbacks = feedbacks != null ? feedbacks : new ArrayList<>();
        }
    }

    public static class IndexedSourceSinkDef {
        public String targetName;
        public SourceSink logic;
        public List<FeedbackOperator> feedbacks;

        @JsonCreator
        public IndexedSourceSinkDef(@JsonProperty("targetName") String targetName, 
                                   @JsonProperty("logic") SourceSink logic, 
                                   @JsonProperty("feedbacks") List<FeedbackOperator> feedbacks) {
            this.targetName = targetName;
            this.logic = logic;
            this.feedbacks = feedbacks != null ? feedbacks : new ArrayList<>();
        }
    }
}
