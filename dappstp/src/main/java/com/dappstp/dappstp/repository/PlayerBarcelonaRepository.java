package com.dappstp.dappstp.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dappstp.dappstp.model.PlayerBarcelona;
import com.dappstp.dappstp.model.PlayerProfileScraping;

@Repository
public interface PlayerBarcelonaRepository  extends JpaRepository<PlayerBarcelona, Long> {

}
