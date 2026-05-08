package org.kinecore.server.domain;

/**
 * Lifecycle states of a simulation job.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
