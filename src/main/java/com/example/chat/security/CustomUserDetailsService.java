package com.example.chat.security;

import com.example.chat.domain.user.UserDto;
import com.example.chat.domain.user.UserMapper;
import com.example.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@NullMarked
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserDto userDto = userRepository.findByEmail(email).map(UserMapper::fromEntityToDto)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        return UserMapper.fromDtoToCustomUserDetails(userDto);
    }
}
