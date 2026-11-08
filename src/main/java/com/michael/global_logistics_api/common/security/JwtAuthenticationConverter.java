package com.michael.global_logistics_api.common.security;

import com.michael.global_logistics_api.identity.domain.UserRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;
import java.util.UUID;

public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        UserRole userRole = UserRole.valueOf(role);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + userRole.name()));

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        authentication.setDetails(new CurrentUser(
                UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString("org_id")),
                userRole,
                jwt.getClaimAsString("email")
        ));

        return authentication;
    }
}
