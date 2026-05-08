package org.kinecore.server.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks a Monte Carlo simulation execution.
 */
@Entity
@Table(name = "simulation_jobs")
public class SimulationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String modelName;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private int progress;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String modelDefinitionJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    private String errorMessage;

    public SimulationJob() {}

    // Manual Builder Pattern
    public static class Builder {
        private final SimulationJob job = new SimulationJob();
        public Builder modelName(String val) { job.modelName = val; return this; }
        public Builder status(JobStatus val) { job.status = val; return this; }
        public Builder startTime(LocalDateTime val) { job.startTime = val; return this; }
        public Builder modelDefinitionJson(String val) { job.modelDefinitionJson = val; return this; }
        public SimulationJob build() { return job; }
    }

    public static Builder builder() { return new Builder(); }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getModelDefinitionJson() { return modelDefinitionJson; }
    public void setModelDefinitionJson(String modelDefinitionJson) { this.modelDefinitionJson = modelDefinitionJson; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
