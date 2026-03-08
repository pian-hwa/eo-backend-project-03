package com.example.chat.service;

import com.example.chat.domain.user.UserDto;
import com.example.chat.domain.user.UserEntity;
import com.example.chat.domain.user.UserMapper;
import com.example.chat.domain.user.jwt.JwtDto;
import com.example.chat.repository.UserRepository;
import com.example.chat.security.jwt.JwtProvider;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Override
    public void create(@NotNull UserDto.Create userDto) {
        checkEmailAvailability(userDto.email());

        UserEntity savedEntity = userRepository.save(
                UserMapper.fromCreateToEntity(setEncodedPassword(userDto))
        );
    }

    @Override
    public Optional<UserDto.Response> read(@NotNull String id) {
        return userRepository.findById(id).map(UserMapper::fromEntityToResponse);
    }

    @Override
    public Optional<UserDto.Response> readByEmail(String email) {
        return userRepository.findByEmail(email).map(UserMapper::fromEntityToResponse);
    }

    @Override
    public Optional<UserDto.Response> update(UserDto.UpdatePassword userDto) {
        UserEntity savedEntity = userRepository.save(
                UserMapper.from(setEncodedPassword(userDto))
        );
    }

    @Override
    public Optional<UserDto.Response> update(UserDto.UpdateInfo userDto) {
        return Optional.empty();
    }

    @Override
    public Optional<UserDto.Response> update(UserDto.UpdateRole userDto) {
        return Optional.empty();
    }

    @Override
    public Optional<UserDto.Response> update(UserDto.UpdatePlan userDto) {
        return Optional.empty();
    }

    @Override
    public boolean delete(String id) {
        return false;
    }

    @Override
    public JwtDto.Response login(UserDto.Login userDto) {
        return null;
    }

    private void checkEmailAvailability(@NotNull String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already in use");
        }
    }

    public UserDto.Create setEncodedPassword(@NotNull UserDto.Create userDto) {
        String encodedPassword = passwordEncoder.encode(userDto.password());

        return new UserDto.Create(
                userDto.email(),
                encodedPassword,
                userDto.username()
        );
    }

    public UserDto.UpdatePassword setEncodedPassword(@NotNull UserDto.UpdatePassword userDto) {
        String encodedPassword = passwordEncoder.encode(userDto.password());

        return new UserDto.UpdatePassword(
                encodedPassword
        );
    }
}
