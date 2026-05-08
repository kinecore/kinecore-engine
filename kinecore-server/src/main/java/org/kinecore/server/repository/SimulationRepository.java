package org.kinecore.server.repository;

import org.kinecore.server.domain.SimulationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for SimulationJob persistence.
 */
@Repository
public interface SimulationRepository extends JpaRepository<SimulationJob, String> {
}
