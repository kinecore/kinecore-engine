package org.kinecore.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kinecore.engine.ModelDefinition;
import org.kinecore.serialization.ModelLoader;
import org.kinecore.solver.MonteCarloEnsemble;
import org.kinecore.server.domain.JobStatus;
import org.kinecore.server.domain.SimulationJob;
import org.kinecore.server.repository.SimulationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Orchestrates simulation runs and interacts with the KineCore Engine.
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final SimulationRepository repository;
    private final ObjectMapper objectMapper;

    public SimulationService(SimulationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Submits a new simulation job to the queue.
     * @param modelName name of the model
     * @param jsonDefinition model definition in JSON
     * @return the created job
     */
    public SimulationJob submitJob(String modelName, String jsonDefinition) {
        SimulationJob job = SimulationJob.builder()
                .modelName(modelName)
                .modelDefinitionJson(jsonDefinition)
                .status(JobStatus.PENDING)
                .startTime(LocalDateTime.now())
                .build();
        
        job = repository.save(job);
        
        // Trigger async execution
        executeJob(job.getId());
        
        return job;
    }

    /**
     * Asynchronously executes the simulation.
     * @param jobId ID of the job to run
     */
    @Async
    public void executeJob(String jobId) {
        SimulationJob job = repository.findById(jobId).orElseThrow();
        
        try {
            job.setStatus(JobStatus.RUNNING);
            repository.save(job);

            // 1. Load Model from JSON (using KineCore Engine)
            ModelDefinition model = ModelLoader.fromJson(job.getModelDefinitionJson());

            // 2. Configure Ensemble (Standard defaults for now)
            MonteCarloEnsemble ensemble = new MonteCarloEnsemble.Builder()
                    .model(model)
                    .sampler(seed -> java.util.Map.of()) // Simplified for now
                    .timeRange(0, 100)
                    .iterations(1000)
                    .build();

            // 3. Run Simulation
            MonteCarloEnsemble.SimulationResult result = ensemble.run();

            // 4. Save Results
            job.setResultJson(objectMapper.writeValueAsString(result));
            job.setStatus(JobStatus.COMPLETED);
            job.setEndTime(LocalDateTime.now());
            job.setProgress(100);
            
        } catch (Exception e) {
            log.error("Simulation failed for job {}: {}", jobId, e.getMessage());
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        } finally {
            repository.save(job);
        }
    }
}
