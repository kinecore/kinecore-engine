package org.kinecore;

import org.kinecore.engine.*;
import org.kinecore.solver.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

/**
 * Analytical Benchmarks (Task 5).
 */
public class BenchmarksTest {

    @Test
    public void testExponentialGrowth() {
        // Model: dy/dt = 1 * y  =>  y(t) = y0 * e^t
        CompartmentalNetworkBuilder builder = new CompartmentalNetworkBuilder();
        builder.addCompartment("Y", 1.0);
        builder.addSource("Y", (t, state, params) -> state[0]); // dy/dt = y
        
        ModelDefinition model = builder.buildDefinition();
        
        MonteCarloEnsemble ensemble = new MonteCarloEnsemble.Builder()
                .model(model)
                .sampler(seed -> Map.of())
                .timeRange(0, 1)
                .stepSize(1.0)
                .iterations(1)
                .addOutput("Y", state -> state[0])
                .build();
        
        MonteCarloEnsemble.SimulationResult result = ensemble.run();
        double finalY = result.getPoints("Y").get(1).p50;
        
        // At t=1, y = e^1 ≈ 2.718
        assertEquals(Math.E, finalY, 0.001);
    }
}
