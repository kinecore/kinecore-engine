package org.kinecore;

import org.kinecore.engine.*;
import org.kinecore.solver.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

/**
 * Mass Conservation Tests (Task 5).
 */
public class ConservationTest {

    @Test
    public void testClosedSystemConservation() {
        // Simple closed system: A -> B
        CompartmentalNetworkBuilder builder = new CompartmentalNetworkBuilder();
        builder.addCompartment("A", 100.0)
               .addCompartment("B", 0.0);
        
        builder.addConstantRateFlux("A", "B", 0.5);
        
        ModelDefinition model = builder.buildDefinition();
        
        MonteCarloEnsemble ensemble = new MonteCarloEnsemble.Builder()
                .model(model)
                .sampler(seed -> Map.of())
                .timeRange(0, 10)
                .stepSize(1.0)
                .iterations(1)
                .addOutput("Total", state -> state[0] + state[1])
                .build();
        
        MonteCarloEnsemble.SimulationResult result = ensemble.run();
        
        for (var p : result.getPoints("Total")) {
            assertEquals(100.0, p.p50, 1e-9, "Mass must be conserved in a closed system");
        }
    }
}
