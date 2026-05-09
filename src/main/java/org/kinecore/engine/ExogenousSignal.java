package org.kinecore.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.kinecore.engine.signal.SplineSignal;

/**
 * Represents a normalized exogenous signal for injection into ODE feedback loops.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SplineSignal.class, name = "spline")
})
@FunctionalInterface
public interface ExogenousSignal {
    double getValueAt(double t, int ageIdx);
}
