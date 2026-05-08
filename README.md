# KineCore Engine

A universal, high-performance system dynamics engine for Java.

## Features
- **Generic Compartmental Modeling**: Build demographic, epidemiological, or climate models using a unified API.
- **Fluent Builder DSL**: Declaratively define compartments, fluxes, and sources.
- **Non-Linear Feedback**: Implement complex tipping points and localized feedback loops.
- **Monte Carlo Ensembles**: Parallel stochastic simulations with streaming quantile aggregation (T-Digest).
- **Sensitivity Analysis**: Pearson Correlation engine to identify key system drivers.
- **Numerical Safety**: Built-in non-negativity constraints for physical systems.
- **Pluggable Solvers**: Support for adaptive solvers like Dormand-Prince and Gragg-Bulirsch-Stoer.

## Quick Start (SIR Model)

```java
ModelDefinition sir = new CompartmentalNetworkBuilder()
    .addCompartment("S", 999_000)
    .addCompartment("I", 1_000)
    .addCompartment("R", 0)
    .addFlux("S", "I", (t, s, state, params) -> 0.35 * s * state[1] / 1e6)
    .addConstantRateFlux("I", "R", 0.1)
    .clampAtZero(true)
    .buildDefinition();

MonteCarloEnsemble ensemble = new MonteCarloEnsemble.Builder()
    .model(sir)
    .sampler(seed -> Map.of("beta", 0.35))
    .timeRange(0, 150)
    .iterations(1000)
    .addOutput("Infected", state -> state[1])
    .build();

SimulationResult result = ensemble.run();
result.toCSV("sir_output.csv");
```

## Installation
Add the following to your `pom.xml`:
```xml
<dependency>
    <groupId>io.github.kinecore</groupId>
    <artifactId>kinecore-engine</artifactId>
    <version>1.0.0</version>
</dependency>
```
