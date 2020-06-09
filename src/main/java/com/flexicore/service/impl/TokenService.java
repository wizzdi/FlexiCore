package com.flexicore.service.impl;

import com.flexicore.constants.Constants;
import com.flexicore.interfaces.FlexiCoreService;
import com.flexicore.model.Baseclass;
import com.flexicore.model.User;
import com.flexicore.response.JWTClaims;
import com.flexicore.response.impl.JWTClaimsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.io.FileUtils;

import javax.enterprise.context.ApplicationScoped;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;



@Primary
@Component
public class TokenService implements com.flexicore.service.TokenService {


    private static String cachedJWTSecret=null;

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

    private static String getJWTSecret() {
        if(cachedJWTSecret==null){
            File file=new File(Constants.jwtTokenSecretLocation);
            if(file.exists()){
                try {
                    cachedJWTSecret=FileUtils.readFileToString(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(cachedJWTSecret==null||cachedJWTSecret.isEmpty()){
                cachedJWTSecret= Baseclass.getBase64ID();
                try {
                    FileUtils.write(file,cachedJWTSecret);
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
