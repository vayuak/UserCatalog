package com.UserCatalogServiceOne.UserCatalog.Services;

import com.UserCatalogServiceOne.UserCatalog.Models.CustomUserDetails;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import com.UserCatalogServiceOne.UserCatalog.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Ghost Identity vectors unmapped: " + username));
        return new CustomUserDetails(user);
    }
}