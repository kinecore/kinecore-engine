package org.kinecore.engine;

import java.util.Map;

/**
 * A stateless definition of a compartmental model that can be bound to parameters
 * to create a runnable instance.
 */
@FunctionalInterface
public interface ModelDefinition {

    /**
     * Binds the model definition to a specific set of parameters.
     *
     * @param params parameter map (e.g. from Monte Carlo sampling)
     * @return a bound, runnable compartmental network
     */
    CompartmentalNetwork bind(Map<String, Double> params);
}
