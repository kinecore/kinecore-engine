package org.kinecore.server.controller;

import org.kinecore.server.domain.SimulationJob;
import org.kinecore.server.repository.SimulationRepository;
import org.kinecore.server.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for KineCore Simulation Hub.
 */
@RestController
@RequestMapping("/api/simulations")
@CrossOrigin(origins = "*") // For Dashboard integration
public class SimulationController {

    private final SimulationService service;
    private final SimulationRepository repository;

    public SimulationController(SimulationService service, SimulationRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    /**
     * Triggers a new simulation.
     * @param request simulation request body
     * @return the created job
     */
    @PostMapping
    public ResponseEntity<SimulationJob> runSimulation(@RequestBody SimulationRequest request) {
        SimulationJob job = service.submitJob(request.getModelName(), request.getJsonModel());
        return ResponseEntity.ok(job);
    }

    /**
     * Lists all simulation jobs.
     * @return list of jobs
     */
    @GetMapping
    public List<SimulationJob> listJobs() {
        return repository.findAll();
    }

    /**
     * Gets a single job by ID.
     * @param id job ID
     * @return the job or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<SimulationJob> getJob(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request DTO for simulation.
     */
    public static class SimulationRequest {
        private String modelName;
        private String jsonModel;

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public String getJsonModel() { return jsonModel; }
        public void setJsonModel(String jsonModel) { this.jsonModel = jsonModel; }
    }
}
