package org.kinecore.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kinecore.engine.CompartmentalNetworkBuilder;
import org.kinecore.engine.ModelDefinition;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Loads model definitions from JSON (Patch 4).
 */
public class ModelLoader {

    /** Constructor */
    public ModelLoader() {}

    /**
     * DTO for the full model.
     */
    public static class JsonModel {
        /** List of compartments */
        public List<JsonCompartment> compartments;
        /** List of fluxes */
        public List<JsonFlux> fluxes;
        /** List of sources/sinks */
        public List<JsonSourceSink> sources;
        /** Map of named feedbacks */
        public Map<String, JsonFeedback> feedbacks;

        /** Constructor */
        public JsonModel() {}
    }

    /**
     * DTO for a compartment.
     */
    public static class JsonCompartment {
        /** Name of the compartment */
        public String name;
        /** Initial value */
        public double initialValue;

        /** Constructor */
        public JsonCompartment() {}
    }

    /**
     * DTO for a flux.
     */
    public static class JsonFlux {
        /** From, To, and Type */
        public String from, to, type;
        /** Base rate */
        public double rate;
        /** List of selective feedbacks */
        @JsonProperty("feedbacks")
        public List<String> feedbackNames;

        /** Constructor */
        public JsonFlux() {}
    }

    /**
     * DTO for a source/sink.
     */
    public static class JsonSourceSink {
        /** Target compartment and Type */
        public String compartment, type;
        /** Base rate */
        public double rate;
        /** List of selective feedbacks */
        @JsonProperty("feedbacks")
        public List<String> feedbackNames;

        /** Constructor */
        public JsonSourceSink() {}
    }

    /**
     * DTO for a feedback operator.
     */
    public static class JsonFeedback {
        /** Type of feedback */
        public String type;
        /** Parameters for the feedback */
        public Map<String, Double> params;

        /** Constructor */
        public JsonFeedback() {}
    }

    /**
     * Loads a model from a JSON string.
     * @param json the JSON content
     * @return a ModelDefinition
     * @throws IOException if parsing fails
     */
    public static ModelDefinition fromJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonModel model = mapper.readValue(json, JsonModel.class);
        
        CompartmentalNetworkBuilder builder = new CompartmentalNetworkBuilder();
        
        for (JsonCompartment c : model.compartments) {
            builder.addCompartment(c.name, c.initialValue);
        }
        
        // This is a simplified mapper. In a real system, you'd have a registry
        // of Flux/SourceSink/Feedback types.
        for (JsonFlux f : model.fluxes) {
            builder.addConstantRateFlux(f.from, f.to, f.rate, 
                f.feedbackNames != null ? f.feedbackNames.toArray(new String[0]) : null);
        }
        
        // ... similar logic for sources and feedbacks would be implemented here
        
        return builder.buildDefinition();
    }
}
