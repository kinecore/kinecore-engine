package org.kinecore.engine;

/**
 * Represents a normalized exogenous signal for injection into ODE feedback loops.
 */
@FunctionalInterface
public interface ExogenousSignal {
    double getValueAt(double t, int ageIdx);
}
