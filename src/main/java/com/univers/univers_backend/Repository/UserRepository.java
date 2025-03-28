package com.univers.univers_backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.univers.univers_backend.Entity.User;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    User findByVerificationCode(String code);
}
