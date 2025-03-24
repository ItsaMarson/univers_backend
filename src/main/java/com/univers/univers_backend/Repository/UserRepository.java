package com.univers.univers_backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.univers.univers_backend.Entity.User;

public interface UserRepository extends JpaRepository<User, Integer>{
    public User findByEmail(String email);
}
