package com.ssafy.myissue.user.filter;

import com.ssafy.myissue.user.domain.User;
import com.ssafy.myissue.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@RequiredArgsConstructor
public class DeviceAuthenticationProvider implements AuthenticationProvider {
    private final UserRepository userRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String uuid = (String) authentication.getPrincipal();

        // DB에서 uuid로 사용자 조회
        User user = userRepository.findByUuid(uuid);
        if (user == null) {
            userRepository.save(User.newOf(uuid));
        }
        user.touch();

        var auths = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        return DeviceAuthenticationToken.authenticated(user.getUuid(), auths);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return DeviceAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
