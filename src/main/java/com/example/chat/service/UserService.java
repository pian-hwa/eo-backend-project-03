package com.example.chat.service;

import com.example.chat.domain.user.UserDto;
import com.example.chat.domain.user.jwt.JwtDto;

import java.util.Optional;

public interface UserService {
    void create(UserDto.Create userDto);
    Optional<UserDto.Response> read(String id);
    Optional<UserDto.Response> readByEmail(String email);
    Optional<UserDto.Response> update(UserDto.UpdatePassword userDto);
    Optional<UserDto.Response> update(UserDto.UpdateInfo userDto);
    Optional<UserDto.Response> update(UserDto.UpdateRole userDto);
    Optional<UserDto.Response> update(UserDto.UpdatePlan userDto);
    boolean delete(String id);
    JwtDto.Response login(UserDto.Login userDto);
}
