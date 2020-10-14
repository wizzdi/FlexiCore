package com.flexicore.service.impl;

import com.flexicore.model.Baseclass;
import com.flexicore.model.User;
import com.flexicore.response.JWTClaims;
import com.flexicore.response.impl.JWTClaimsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


@Primary
@Component
public class TokenService implements com.flexicore.service.TokenService {


    @Autowired
    private String cachedJWTSecret;


    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public String cachedJWTSecret(){
        return getJWTSecret();
    }
    @Value("${flexicore.security.jwt.secretLocation:/home/flexicore/jwt.secret}")
    private String jwtTokenSecretLocation;

    @Override
    public String getJwtToken(User user, OffsetDateTime expirationDate){
        return getJwtToken(user,expirationDate,null,null);
    }
    @Override
    public String getJwtToken(User user, OffsetDateTime expirationDate, String writeTenant, Set<String> readTenants) {

        Map<String, Object> claims=new HashMap<>();
        if(writeTenant!=null){
            claims.put(WRITE_TENANT,writeTenant);
        }
        if(readTenants!=null && !readTenants.isEmpty()){
            claims.put(READ_TENANTS,readTenants);
        }
        return  Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuer(ISSUER)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expirationDate.toInstant()))
                .signWith(SignatureAlgorithm.HS512,getJWTSecret())
                .addClaims(claims)
                .compact();
    }

    private String getJWTSecret() {
        if(cachedJWTSecret==null){
            File file=new File(jwtTokenSecretLocation);
            if(file.exists()){
                try {
                    cachedJWTSecret=FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(cachedJWTSecret==null||cachedJWTSecret.isEmpty()){
                cachedJWTSecret= Baseclass.getBase64ID();
                try {
                    FileUtils.write(file,cachedJWTSecret,StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return cachedJWTSecret;


    }

    @Override
    public JWTClaims parseClaimsAndVerifyClaims(String jwtToken, Logger logger) {
        Claims claims =null;
        try {
            claims=Jwts.parser().setSigningKey(getJWTSecret()).parseClaimsJws(jwtToken).getBody();
        }
        catch (JwtException e){
            logger.log(Level.SEVERE,"invalid token ",e);
        }
        return claims!=null&&claims.getIssuer().equals(ISSUER)?new JWTClaimsImpl(claims):null;



    }

}
