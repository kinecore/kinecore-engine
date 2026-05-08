package org.kinecore.demo;

import org.kinecore.engine.*;
import org.kinecore.solver.*;
import java.util.Map;

/**
 * Energy Balance Model with Ice-Albedo Feedback (Task 6).
 */
public class ClimateSimulation {
    /** Constructor */
    public ClimateSimulation() {}

    /**
     * Main entry point.
     * @param args arguments
     */
    public static void main(String[] args) {
        CompartmentalNetworkBuilder builder = new CompartmentalNetworkBuilder();
        
        // Temperature compartment (Initial 288 K)
        builder.addCompartment("Temperature", 288.0);
        
        // Parameters
        double solarConstant = 1361.0 / 4.0;
        double sigma = 5.67e-8; // Stefan-Boltzmann
        double emissivity = 0.61;
        double heatCapacity = 1e8; // Shallow ocean
        
        // Ice-Albedo Feedback: Albedo drops as temperature rises
        builder.addFeedback("ice_albedo", (t, flow, state, params) -> {
            double temp = state[0];
            double albedo = temp > 290 ? 0.3 : 0.6;
            return flow * (1 - albedo);
        });
        
        // Shortwave absorption (Source)
        builder.addSource("Temperature", (t, state, params) -> solarConstant / heatCapacity, "ice_albedo");
        
        // Longwave emission (Sink)
        builder.addSource("Temperature", (t, state, params) -> {
            double temp = state[0];
            return -(emissivity * sigma * Math.pow(temp, 4)) / heatCapacity;
        });
        
        ModelDefinition model = builder.buildDefinition();
        
        MonteCarloEnsemble ensemble = new MonteCarloEnsemble.Builder()
                .model(model)
                .sampler(seed -> Map.of())
                .timeRange(0, 50)
                .stepSize(1.0)
                .iterations(1)
                .addOutput("Temp", state -> state[0])
                .build();
        
        MonteCarloEnsemble.SimulationResult result = ensemble.run();
        System.out.println("Time | Temperature (K)");
        for (var p : result.getPoints("Temp")) {
            System.out.printf("%.0f | %.2f%n", p.time, p.p50);
        }
    }
}
