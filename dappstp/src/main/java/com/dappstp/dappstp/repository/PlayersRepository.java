package com.dappstp.dappstp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dappstp.dappstp.model.Players;

@Repository
public interface PlayersRepository  extends JpaRepository<Players, Long> {

}
