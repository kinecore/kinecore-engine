package org.kinecore.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.kinecore.engine.Model;
import org.kinecore.engine.ModelDefinition;

import java.io.IOException;

/**
 * Enterprise-grade loader for System Dynamics models.
 * 
 * <p>Leverages Jackson's polymorphic deserialization to inflate complex model
 * structures (including non-linear feedbacks and stochastic sources) directly
 * from JSON payloads submitted by the React dashboard.</p>
 */
public class ModelLoader {

    private final ObjectMapper mapper;

    public ModelLoader() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Deserializes a model from JSON.
     * 
     * @param json model structure JSON
     * @return a concrete Model instance that implements ModelDefinition
     * @throws IOException if the JSON is malformed or types are missing
     */
    public Model load(String json) throws IOException {
        return mapper.readValue(json, Model.class);
    }

    /**
     * Serializes a model to JSON.
     * 
     * @param model the model to save
     * @return JSON string representation
     * @throws IOException if serialization fails
     */
    public String save(Model model) throws IOException {
        return mapper.writeValueAsString(model);
    }

    /**
     * Static convenience method for quick loading.
     * 
     * @param json the JSON content
     * @return a ModelDefinition
     * @throws IOException if parsing fails
     */
    public static ModelDefinition fromJson(String json) throws IOException {
        return new ModelLoader().load(json);
    }
}
