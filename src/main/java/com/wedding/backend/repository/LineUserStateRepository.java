package com.wedding.backend.repository;

import com.wedding.backend.domain.LineUserState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LineUserStateRepository extends JpaRepository<LineUserState, String> {
}
