package com.ssafy.myissue.user.filter;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class DeviceAuthenticationToken extends AbstractAuthenticationToken {
    private final String principal;

    // 인증 정보가 없는 경우
    public DeviceAuthenticationToken(String deviceUuid) {
        super(null);
        this.principal = deviceUuid;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
