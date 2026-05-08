package org.kinecore.demo;

import org.kinecore.engine.*;
import org.kinecore.solver.*;
import org.kinecore.solver.impl.DormandPrince853Solver;

import java.util.Arrays;
import java.util.Map;

/**
 * Validates the KineCore library by implementing the Kerala Demographic Model
 * using the new ModelDefinition, ODESolver, and modular Monte Carlo components.
 */
public class KeralaSimulation {

    /** Constructor */
    public KeralaSimulation() {}

    /**
     * Main entry point.
     * @param args arguments
     */
    public static void main(String[] args) {
        // 1. Build the model definition (stateless)
        ModelDefinition keralaModel = createKeralaModel();

        // 2. Define the Monte Carlo ensemble
        MonteCarloEnsemble ensemble = new MonteCarloEnsemble.Builder()
                .model(keralaModel)
                // Sample parameters (Patch 5: ParameterSampler)
                .sampler(seed -> {
                    var rng = new org.apache.commons.math3.random.MersenneTwister(seed);
                    return Map.of(
                        "fertility_scale", rng.nextGaussian() * 0.05 + 1.0,
                        "mortality_scale", rng.nextGaussian() * 0.03 + 1.0
                    );
                })
                // Use a pluggable solver (Patch 2: ODESolver)
                .solver(new DormandPrince853Solver(1e-6, 1.0, 1e-8, 1e-8))
                .timeRange(2020, 2120)
                .stepSize(1.0)
                .iterations(1000)
                // Multi-metric tracking (Gap 3 fix)
                .addOutput("Total_Population", state -> Arrays.stream(state).sum())
                .addOutput("Elderly_Population", state -> Arrays.stream(state, 65, 101).sum())
                .build();

        // 3. Run the simulation
        MonteCarloEnsemble.SimulationResult result = ensemble.run();

        // 4. Output results
        System.out.println("Year | Median Pop | p5 Pop | p95 Pop");
        for (var p : result.getPoints("Total_Population")) {
            System.out.printf("%.0f | %.0f | %.0f | %.0f%n", p.time, p.p50, p.p5, p.p95);
        }
    }

    private static ModelDefinition createKeralaModel() {
        CompartmentalNetworkBuilder builder = new CompartmentalNetworkBuilder();

        // Add age compartments (0..100)
        for (int i = 0; i <= 100; i++) {
            builder.addCompartment("age_" + i, 50000.0);
        }

        // Global Feedback: Elderly Dependency Ratio (Gap 1 fix)
        builder.addFeedback("edr_stress", (t, flow, state, params) -> {
            double workers = Arrays.stream(state, 15, 65).sum();
            double elderly = Arrays.stream(state, 65, 101).sum();
            double ratio = workers == 0 ? 1 : elderly / workers;
            double stress = Math.max(0, ratio - 0.35);
            return flow * Math.max(0.35, Math.exp(-2.5 * stress));
        });

        // Aging Fluxes (Selective Feedback: No feedback on aging!)
        for (int i = 0; i < 100; i++) {
            builder.addConstantRateFlux("age_" + i, "age_" + (i + 1), 1.0);
        }

        // Mortality Sinks
        for (int i = 0; i <= 100; i++) {
            final int age = i;
            builder.addSource("age_" + i, (t, state, params) -> {
                double baseMortality = 0.01; // simplified
                return -baseMortality * state[age] * params.getOrDefault("mortality_scale", 1.0);
            });
        }

        // Fertility Source (Applies EDR stress feedback!)
        builder.addSource("age_0", (t, state, params) -> {
            double births = 0;
            for (int a = 15; a <= 49; a++) {
                births += 0.05 * state[a]; // simplified TFR
            }
            return births * params.getOrDefault("fertility_scale", 1.0);
        }, "edr_stress");

        return builder.buildDefinition();
    }
}
