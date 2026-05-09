package org.kinecore.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.kinecore.engine.flux.ConstantRateFlux;
import org.kinecore.engine.flux.MassActionFlux;
import org.kinecore.engine.flux.ParameterDrivenFlux;

import java.util.Map;

/**
 * A directed flow of mass from one compartment to another.
 *
 * <p>This interface is polymorphically deserializable by Jackson via the {@code "type"}
 * discriminator field, allowing REST clients (e.g., the KineCore dashboard) to submit
 * model definitions over a JSON API.</p>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConstantRateFlux.class,       name = "constant"),
    @JsonSubTypes.Type(value = MassActionFlux.class,         name = "massAction"),
    @JsonSubTypes.Type(value = ParameterDrivenFlux.class,    name = "paramDriven")
})
@FunctionalInterface
public interface Flux {

    /**
     * Computes the flow rate from the source compartment at time {@code t}.
     *
     * @param t         current simulation time
     * @param fromValue current amount in the source compartment ({@code state[fromIdx]})
     * @param state     full system state vector (read-only)
     * @param params    parameters from the current simulation environment (e.g., Monte Carlo)
     * @return          amount transferred per unit time (must be ≥ 0)
     */
    double getFlowRate(double t, double fromValue, double[] state, Map<String, Double> params);
}
