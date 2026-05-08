package org.kinecore.demo;

import org.kinecore.engine.*;
import org.kinecore.solver.*;

import java.util.Map;

/**
 * Standard SIR (Susceptible-Infected-Recovered) Epidemiological Model (Task 6).
 */
public class SIRSimulation {
    /** Constructor */
    public SIRSimulation() {}

    /**
     * Main entry point.
     * @param args arguments
     */
    public static void main(String[] args) {
        CompartmentalNetworkBuilder builder = new CompartmentalNetworkBuilder();
        
        // 1. Setup Compartments
        builder.addCompartment("S", 999_000)
               .addCompartment("I", 1_000)
               .addCompartment("R", 0);

        // 2. Setup Fluxes (beta * S * I / N)
        double beta = 0.35;
        double gamma = 0.1;
        
        builder.addFlux("S", "I", (t, s, state, params) -> {
            double totalN = state[0] + state[1] + state[2];
            return (beta * s * state[1]) / totalN;
        });
        
        builder.addConstantRateFlux("I", "R", gamma);
        
        // 3. Numerical Safety
        builder.clampAtZero(true);
        
        ModelDefinition model = builder.buildDefinition();
        
        // 4. Run Monte Carlo with parameter uncertainty
        MonteCarloEnsemble ensemble = new MonteCarloEnsemble.Builder()
                .model(model)
                .sampler(seed -> Map.of("beta", 0.35, "gamma", 0.1)) // Deterministic for this demo
                .timeRange(0, 150)
                .stepSize(1.0)
                .iterations(1)
                .addOutput("Infected", state -> state[1])
                .addOutput("Recovered", state -> state[2])
                .build();
        
        MonteCarloEnsemble.SimulationResult result = ensemble.run();
        
        System.out.println("Day | Infected | Recovered");
        for (int i = 0; i < result.getPoints("Infected").size(); i++) {
            var pI = result.getPoints("Infected").get(i);
            var pR = result.getPoints("Recovered").get(i);
            System.out.printf("%.0f | %.0f | %.0f%n", pI.time, pI.p50, pR.p50);
        }
    }
}
