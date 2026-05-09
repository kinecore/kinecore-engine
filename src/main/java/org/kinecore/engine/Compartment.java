package org.kinecore.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents a single population or state variable in the system.
 * 
 * <p>Supports parameterized initial values via {@code initialValueParamKey}, 
 * allowing starting states to be sampled in Monte Carlo iterations.</p>
 */
public class Compartment {

    private final String name;
    private final int index;
    private final Function<Map<String, Double>, Double> initialValueSupplier;
    private final String initialValueParamKey;
    
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private boolean derivativeLocked = false;

    /**
     * Constructs a compartment with a fixed initial value.
     * @param name                 human-readable name
     * @param index                position in the ODE state vector
     * @param initialValue         fixed starting value
     */
    public Compartment(String name, int index, double initialValue) {
        this(name, index, initialValue, null);
    }

    /**
     * Internal constructor for Jackson.
     */
    @JsonCreator
    public Compartment(@JsonProperty("name") String name, 
                       @JsonProperty("index") int index, 
                       @JsonProperty("initialValue") double initialValue,
                       @JsonProperty("initialValueParamKey") String initialValueParamKey) {
        this.name = name;
        this.index = index;
        this.initialValueParamKey = initialValueParamKey;
        
        if (initialValueParamKey != null && !initialValueParamKey.isEmpty()) {
            this.initialValueSupplier = params -> params.getOrDefault(initialValueParamKey, initialValue);
        } else {
            this.initialValueSupplier = params -> initialValue;
        }
    }

    /**
     * Constructs a compartment with a parameterized initial value.
     * @param name                 human-readable name
     * @param index                position in the ODE state vector
     * @param initialValueSupplier function of parameters
     */
    public Compartment(String name, int index, Function<Map<String, Double>, Double> initialValueSupplier) {
        this.name = name;
        this.index = index;
        this.initialValueSupplier = initialValueSupplier;
        this.initialValueParamKey = null;
    }

    /**
     * Gets the human-readable name of this compartment.
     * @return the name
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Gets the position in the ODE state vector.
     * @return the index
     */
    @JsonProperty("index")
    public int getIndex() {
        return index;
    }

    /**
     * Gets the parameter key for the initial value, if any.
     * @return parameter key or null
     */
    @JsonProperty("initialValueParamKey")
    public String getInitialValueParamKey() {
        return initialValueParamKey;
    }

    /**
     * Calculates the initial value for a specific simulation instance.
     * @param params parameters for the simulation
     * @return the initial value
     */
    public double getInitialValue(Map<String, Double> params) {
        return initialValueSupplier.apply(params);
    }

    @JsonProperty("min")
    public double getMin() { return min; }
    public Compartment withMin(double min) { this.min = min; return this; }

    @JsonProperty("max")
    public double getMax() { return max; }
    public Compartment withMax(double max) { this.max = max; return this; }

    @JsonProperty("derivativeLocked")
    public boolean isDerivativeLocked() { return derivativeLocked; }
    public Compartment lockDerivative(boolean locked) { this.derivativeLocked = locked; return this; }
}
