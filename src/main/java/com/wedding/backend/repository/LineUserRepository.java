package com.wedding.backend.repository;

import com.wedding.backend.domain.LineUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LineUserRepository extends JpaRepository<LineUser, String> {
}
