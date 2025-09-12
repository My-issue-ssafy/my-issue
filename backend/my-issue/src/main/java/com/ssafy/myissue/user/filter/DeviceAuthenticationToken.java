package com.ssafy.myissue.user.filter;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class DeviceAuthenticationToken extends AbstractAuthenticationToken {
    private final String principal; // deviceUuid

    private DeviceAuthenticationToken(String deviceUuid, Collection<? extends GrantedAuthority> auths,boolean authenticated) {
        super(auths);
        this.principal = deviceUuid;
        setAuthenticated(authenticated);
    }

    // 인증 정보가 없는 경우
    public static DeviceAuthenticationToken unauthenticated(String deviceUuid) {
        return new DeviceAuthenticationToken(deviceUuid, null,false);
    }

    // 인증 후
    public static DeviceAuthenticationToken authenticated(String deviceUuid, Collection<? extends GrantedAuthority> auths) {
        return new DeviceAuthenticationToken(deviceUuid, auths,true);
    }

    @Override public Object getCredentials() {
        return "";
    }
    @Override public Object getPrincipal() {
        return this.principal;
    }
}
