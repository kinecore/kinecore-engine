package org.kinecore.engine;

import java.util.Map;
import java.util.function.Function;

/**
 * Represents a single population or state variable in the system.
 */
public class Compartment {

    private final String name;
    private final int index;
    private final Function<Map<String, Double>, Double> initialValueSupplier;

    /**
     * Constructs a compartment with a fixed initial value.
     * @param name                 human-readable name
     * @param index                position in the ODE state vector
     * @param staticInitialValue   fixed starting value
     */
    public Compartment(String name, int index, double staticInitialValue) {
        this(name, index, params -> staticInitialValue);
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
    }

    /**
     * Gets the human-readable name of this compartment.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the position in the ODE state vector.
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Calculates the initial value for a specific simulation instance.
     * @param params parameters for the simulation
     * @return the initial value
     */
    public double getInitialValue(Map<String, Double> params) {
        return initialValueSupplier.apply(params);
    }
}
