package com.dappstp.dappstp.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dappstp.dappstp.model.PlayerProfileScraping;

@Repository
public interface PlayersProfileRepository extends JpaRepository<PlayerProfileScraping, Long> {
    // Repository methods

}

