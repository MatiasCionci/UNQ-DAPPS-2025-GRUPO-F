package com.dappstp.dappstp.repository;

import com.dappstp.dappstp.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    // el método findById(Long) ya viene heredado de JpaRepository
}
