package com.dappstp.dappstp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dappstp.dappstp.model.PlayerBarcelona;

@Repository
public interface PlayerBarcelonaRepository  extends JpaRepository<PlayerBarcelona, Long> {

}
