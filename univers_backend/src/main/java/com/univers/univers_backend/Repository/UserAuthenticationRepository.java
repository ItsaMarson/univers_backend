package com.univers.univers_backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.univers.univers_backend.Entity.UserAuthenticationEntity;

public interface UserAuthenticationRepository extends JpaRepository<UserAuthenticationEntity, Integer>{
    public UserAuthenticationEntity findByEmail(String email);
}
