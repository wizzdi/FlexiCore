package com.flexicore.response.impl;

import com.flexicore.response.JWTClaims;
import io.jsonwebtoken.Claims;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JWTClaimsImpl implements JWTClaims {
    private Claims claims;

    public JWTClaimsImpl(Claims claims) {
        this.claims=claims;
    }

    @Override
    public String getIssuer() {
        return claims.getIssuer();
    }

    @Override
    public JWTClaimsImpl setIssuer(String iss) {
        claims.setIssuer(iss);
        return this;
    }

    @Override
    public String getSubject() {
        return claims.getSubject();
    }

    @Override
    public JWTClaimsImpl setSubject(String sub) {
        claims.setSubject(sub);
        return this;
    }

    @Override
    public String getAudience() {
        return claims.getAudience();
    }

    @Override
    public JWTClaimsImpl setAudience(String aud) {
        claims.setAudience(aud);
        return this;
    }

    @Override
    public Date getExpiration() {
        return claims.getExpiration();
    }

    @Override
    public JWTClaimsImpl setExpiration(Date exp) {
        claims.setExpiration(exp);
        return this;
    }

    @Override
    public Date getNotBefore() {
        return claims.getNotBefore();
    }

    @Override
    public JWTClaimsImpl setNotBefore(Date nbf) {
        claims.setNotBefore(nbf);
        return this;
    }

    @Override
    public Date getIssuedAt() {
        return claims.getIssuedAt();
    }

    @Override
    public JWTClaimsImpl setIssuedAt(Date iat) {
        claims.setIssuedAt(iat);
        return this;
    }

    @Override
    public String getId() {
        return claims.getId();
    }

    @Override
    public JWTClaimsImpl setId(String jti) {
        claims.setId(jti);
        return this;
    }

    @Override
    public <T> T get(String claimName, Class<T> requiredType) {
        return claims.get(claimName, requiredType);
    }

    @Override
    public int size() {
        return claims.size();
    }

    @Override
    public boolean isEmpty() {
        return claims.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return claims.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return claims.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return claims.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return claims.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return claims.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        claims.putAll(m);
    }

    @Override
    public void clear() {
        claims.clear();
    }

    @Override
    public Set<String> keySet() {
        return claims.keySet();
    }

    @Override
    public Collection<Object> values() {
        return claims.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return claims.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return claims.equals(o);
    }

    @Override
    public int hashCode() {
        return claims.hashCode();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return claims.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        claims.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        claims.replaceAll(function);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return claims.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return claims.remove(key, value);
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        return claims.replace(key, oldValue, newValue);
    }

    @Override
    public Object replace(String key, Object value) {
        return claims.replace(key, value);
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
        return claims.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return claims.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return claims.compute(key, remappingFunction);
    }

    @Override
    public Object merge(String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return claims.merge(key, value, remappingFunction);
    }
}
