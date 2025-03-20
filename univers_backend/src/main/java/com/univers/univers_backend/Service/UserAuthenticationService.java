package com.univers.univers_backend.Service;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.univers.univers_backend.Entity.UserAuthenticationEntity;
import com.univers.univers_backend.Repository.UserAuthenticationRepository;


    @Service
    public class UserAuthenticationService {

    @Autowired
    private UserAuthenticationRepository userAuthenticationRepository;


    public UserAuthenticationEntity register(UserAuthenticationEntity registeruser) {
        return userAuthenticationRepository.save(registeruser);
    }

    public List<UserAuthenticationEntity> getAllUserAuthentication() {
        return userAuthenticationRepository.findAll();
    }

    public UserAuthenticationEntity findByEmail(String email) {
        return userAuthenticationRepository.findByEmail(email);
    }

    public UserAuthenticationEntity updateAuthentication(int sid, UserAuthenticationEntity newuserAuthenticationEntity) {

        try {
            UserAuthenticationEntity userAuthenticationEntity = userAuthenticationRepository.findById(sid).get();
            userAuthenticationEntity.setEmail(newuserAuthenticationEntity.getEmail());
            userAuthenticationEntity.setPassword(newuserAuthenticationEntity.getPassword());
            userAuthenticationEntity.setRole(newuserAuthenticationEntity.getRole());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No such user exists" + sid);
        } finally {
            return userAuthenticationRepository.save(newuserAuthenticationEntity);
        }
    }

    public String deleteUserAuthentication(int sid) {
        try {
            userAuthenticationRepository.deleteById(sid);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No such user exists" + sid);
        } finally {
            return "User Deleted";
        }
    }


}