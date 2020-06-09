/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flexicore.service.impl;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import javax.mail.MessagingException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flexicore.constants.Constants;
import com.flexicore.data.BaselinkRepository;
import com.flexicore.data.TenantRepository;
import com.flexicore.data.UserRepository;
import com.flexicore.data.jsoncontainers.*;
import com.flexicore.exceptions.*;
import com.flexicore.model.*;
import com.flexicore.request.*;
import com.flexicore.response.*;
import com.flexicore.security.*;
import com.flexicore.service.TokenService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lambdaworks.crypto.SCryptUtil;
import io.joshworks.restclient.http.HttpResponse;
import io.joshworks.restclient.http.MediaType;
import io.joshworks.restclient.http.RestClient;
import io.joshworks.restclient.http.mapper.ObjectMapper;
import io.joshworks.restclient.http.mapper.ObjectMappers;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class UserService implements com.flexicore.service.UserService {

    private Logger log = Logger.getLogger(getClass().getCanonicalName());

    @Autowired
    private UserRepository userrepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private BaselinkRepository baselinkRepository;

    @Autowired
    private BaseclassNewService baseclassService;

    @Autowired
    private TokenService tokenService;

    private static final int scryptN = 16384;
    private static final int scryptR = 8;
    private static final int scryptP = 1;


    @Override
    public <T extends Baseclass> List<T> getAllFiltered(QueryInformationHolder<T> queryInformationHolder) {
        return userrepository.getAllFiltered(queryInformationHolder);
    }

    @Override
    public User findById(String id) {
        return userrepository.findById(id);
    }

    private static RestClient client;

    /**
     *
     */


    private Cache<String, RunningUser> loggedusers = CacheBuilder.newBuilder().expireAfterAccess(6, TimeUnit.MINUTES).maximumSize(Constants.userCacheMaxSize).build();
    private Cache<String, String> blacklist = CacheBuilder.newBuilder().expireAfterAccess(6, TimeUnit.HOURS).maximumSize(10000).build();


    static {
        ObjectMapper mapper = new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                            .registerModule(new JavaTimeModule());

            @Override
            public <T> T readValue(String s, Class<T> aClass) {
                try {
                    return objectMapper.readValue(s, aClass);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String writeValue(Object o) {
                try {
                    return objectMapper.writeValueAsString(o);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        MediaType mediaType = MediaType.valueOf("application/json; charset=utf-8");
        ObjectMappers.register(MediaType.APPLICATION_JSON_TYPE, mapper);
        ObjectMappers.register(mediaType, mapper);
        ObjectMappers.register(MediaType.APPLICATION_FORM_URLENCODED_TYPE, mapper);


        client = RestClient.builder().build();


    }


    @Override
    public void massMerge(List<?> toMerge) {
        userrepository.massMerge(toMerge);
    }

    /**
     * @param newuser
     * @param securityContext
     * @return AuthenticationKey
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws Exception
     */

    @Override
    public <T extends User> RunningUser register(NewUser<T> newuser, boolean shouldlogin, SecurityContext securityContext)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, Exception {
        Tenant tenant = tenantRepository.getTenantByApiKey(newuser.getApikey());
        return register(newuser, shouldlogin, securityContext, tenant);

    }

    @Override
    public User createUser(UserCreate userCreate, SecurityContext securityContext) {
        List<Object> toMerge = new ArrayList<>();
        User user = createUserNoMerge(userCreate, securityContext);
        toMerge.add(user);
        TenantToUserCreate tenantToUserCreate = new TenantToUserCreate().setDefaultTenant(true).setUser(user).setTenant(userCreate.getTenant());
        TenantToUser tenantToUser = createTenantToUserNoMerge(tenantToUserCreate, securityContext);
        toMerge.add(tenantToUser);
        userrepository.massMerge(toMerge);
        user.getTenantToUsers().add(tenantToUser);
        return user;
    }


    @Override
    public TenantToUser createTenantToUserNoMerge(TenantToUserCreate tenantToUserCreate, SecurityContext securityContext) {
        TenantToUser tenantToUser = new TenantToUser("TenantToUser", securityContext);

        updateTenantToUserNoMerge(tenantToUserCreate, tenantToUser);
        return tenantToUser;
    }

    public boolean updateTenantToUserNoMerge(TenantToUserCreate tenantToUserCreate, TenantToUser tenantToUser) {
        boolean update = baseclassService.updateBaseclassNoMerge(tenantToUserCreate,tenantToUser);
        if (tenantToUserCreate.isDefaultTenant() != null && tenantToUserCreate.isDefaultTenant() != tenantToUser.isDefualtTennant()) {
            tenantToUser.setDefualtTennant(tenantToUserCreate.isDefaultTenant());
            update = true;
        }
        if(tenantToUserCreate.getTenant()!=null && (tenantToUser.getLeftside()==null || !tenantToUserCreate.getTenant().getId().equals(tenantToUser.getLeftside().getId()))){
            tenantToUser.setLeftside(tenantToUserCreate.getTenant());
            update=true;
        }

        if(tenantToUserCreate.getUser()!=null && (tenantToUser.getRightside()==null || !tenantToUserCreate.getUser().getId().equals(tenantToUser.getRightside().getId()))){
            tenantToUser.setUser(tenantToUserCreate.getUser());
            update=true;
        }
        return update;
    }

    @Override
    public User createUserNoMerge(UserCreate createUser, SecurityContext securityContext) {
        User user = new User(createUser.getName(), securityContext);
        updateUserNoMerge(user, createUser);
        return user;
    }

    @Override
    public boolean updateUserNoMerge(User user, UserCreate createUser) {
        boolean update = baseclassService.updateBaseclassNoMerge(createUser, user);

        if (createUser.getEmail() != null && !createUser.getEmail().equals(user.getEmail())) {
            user.setEmail(createUser.getEmail());
            update = true;
        }

        if (createUser.getPhoneNumber() != null && !createUser.getPhoneNumber().equals(user.getPhoneNumber())) {
            user.setPhoneNumber(createUser.getPhoneNumber());
            update = true;
        }

        if (createUser.getUiConfiguration() != null && !createUser.getUiConfiguration().equals(user.getUiConfiguration())) {
            user.setUiConfiguration(createUser.getUiConfiguration());
            update = true;
        }


        if (createUser.getLastName() != null && !createUser.getLastName().equals(user.getSurName())) {
            user.setSurName(createUser.getLastName());
            update = true;
        }

        if (createUser.getDisabled() != null && createUser.getDisabled() != user.isDisabled()) {
            user.setDisabled(createUser.getDisabled());
            update = true;
        }

        if (createUser.getApprovingUser() != null && (user.getApprovingUser() == null || !createUser.getApprovingUser().getId().equals(user.getApprovingUser().getId()))) {
            user.setApprovingUser(createUser.getApprovingUser());
            update = true;
        }

        if (createUser.getDateApproved() != null && !createUser.getDateApproved().equals(user.getDateApproved())) {
            user.setDateApproved(createUser.getDateApproved());
            update = true;
        }


        if (createUser.getPassword() != null) {

            String hash = hashPassword(createUser.getPassword());
            if (!hash.equals(user.getPassword())) {
                user.setPassword(hash);
                update = true;
            }
        }
        return update;
    }

    private String hashPassword(String plain) {
        return SCryptUtil.scrypt(plain, scryptN, scryptR, scryptP);
    }

    /**
     * @param newuser
     * @param shouldlogin
     * @param securityContext
     * @param tenant
     * @param <T>
     * @return
     * @throws Exception
     * @deprecated replaced by {@link #createUser(UserCreate, SecurityContext)}
     */
    @Override
    @Deprecated
    public <T extends User> RunningUser register(NewUser<T> newuser, boolean shouldlogin, SecurityContext securityContext, Tenant tenant)
            throws Exception {

        User existing = newuser.getEmail() != null ? userrepository.findByEmail(newuser.getEmail()) : (newuser.getPhonenumber() != null ? findUserByPhoneNumberOrNull(newuser.getPhonenumber(), null) : null);
        if (existing == null) {

            T user = newuser.getClazz().newInstance().Create(newuser.getName(), securityContext);
            user.setTenant(tenant);
            if (newuser.getApikey() == null) {
                newuser.setApikey("");
            }
            userrepository.addUserToTenantNoPersist(user, tenant, securityContext, true);
            userrepository.register(user);
            userrepository.flush();

            AuthenticationRequestHolder bundle = new AuthenticationRequestHolder(newuser.getEmail(), newuser.getPhonenumber(), newuser.getPassword(),
                    newuser.getApikey());
            RunningUser runninguser;
            if (shouldlogin) {
                runninguser = login(bundle, user);

                return runninguser;
            } else {
                runninguser = new RunningUser();
                runninguser.setUser(user);

                return runninguser;
            }

        } else {
            if (newuser.getEmail() != null) {
                throw new UserCannotBeRegisteredException("Email already taken");

            } else {
                throw new UserCannotBeRegisteredException("PhoneNumber already taken");

            }
        }
    }

    /**
     * @param newuser
     * @param shouldlogin
     * @param securityContext
     * @param <T>
     * @return
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @deprecated replaced by {@link #createUserNoMerge(UserCreate, SecurityContext)}
     */
    @Override
    @Deprecated
    public <T extends User> RunningUser registerNoMerge(NewUser<T> newuser, boolean shouldlogin, SecurityContext securityContext)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Tenant tenant = tenantRepository.getTenantByApiKey(newuser.getApikey());
        return registerNoMerge(newuser, shouldlogin, securityContext, tenant);

    }


    /**
     * @param newuser
     * @param shouldlogin
     * @param securityContext
     * @param tenant
     * @param <T>
     * @return
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @deprecated replaced by {@link #createUserNoMerge(UserCreate, SecurityContext)}
     */
    @Override
    @Deprecated
    public <T extends User> RunningUser registerNoMerge(NewUser<T> newuser, boolean shouldlogin, SecurityContext securityContext, Tenant tenant)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        User existing = userrepository.findByEmail(newuser.getEmail());
        if (existing == null) {

            T user = newuser.getClazz().newInstance().Create(newuser.getName(), securityContext);
            user.setTenant(tenant);
            if (newuser.getApikey() == null) {
                newuser.setApikey("");
            }
            user.setPassword(SCryptUtil.scrypt(newuser.getPassword(), scryptN, scryptR, scryptP));
            user.setPhoneNumber(newuser.getPhonenumber());
            user.setEmail(newuser.getEmail());
            user.setSurName(newuser.getSurname());
            userrepository.addUserToTenantNoPersist(user, tenant, securityContext, true);


            AuthenticationRequestHolder bundle = new AuthenticationRequestHolder(newuser.getEmail(), newuser.getPhonenumber(), newuser.getPassword(),
                    newuser.getApikey());
            RunningUser runninguser;
            if (shouldlogin) {
                runninguser = login(bundle, user);

                return runninguser;
            } else {
                runninguser = new RunningUser();
                runninguser.setUser(user);

                return runninguser;
            }

        } else {
            throw (new UserCannotBeRegisteredException("Email already taken"));
        }
    }

    public int multipleCreate(int number, SecurityContext securityContext) {
        return userrepository.multipleCreate(number, securityContext);

    }

    @Override
    public boolean attachTenant(String authenticationKey, String apiKey, SecurityContext securityContext) {
/*
        Tenant tenant = tenantRepository.getTenantByApiKey(apiKey);
        User user = securityContext.getUser();
        if (tenant != null && user != null) {
            Baselink link = baselinkRepository.findBySides(tenant, user);
            if (link == null) {
                userrepository.addUserToTenant(user, tenant, securityContext, false);


            }
            RunningUser runningUser = loggedusers.getIfPresent(authenticationKey);
            if (runningUser != null) {
                runningUser.getTenants().add(tenant);
                return true;
            }
        }*/
        return false;

    }

    @Override
    public RunningUser getRunningUser(String authenticationKey) {
        if (blacklist.getIfPresent(authenticationKey) != null) {
            return null;
        }
        RunningUser runningUser = loggedusers.getIfPresent(authenticationKey);
        if (runningUser != null) {
            if (runningUser.getExpiresDate() != null && OffsetDateTime.now().isAfter(runningUser.getExpiresDate())) {
                loggedusers.invalidate(authenticationKey);
                return null;
            }
            return runningUser;
        }
        JWTClaims claims = tokenService.parseClaimsAndVerifyClaims(authenticationKey, log);
        if (claims != null) {
            String email = claims.getSubject();
            User user = userrepository.findByEmail(email);
            runningUser = createRunningUser(user, authenticationKey);
            runningUser.setExpiresDate(claims.getExpiration() != null ? claims.getExpiration().toInstant().atZone(ZoneId.of("UTC")).toOffsetDateTime() : null);
            Collection<String> readTenantsRaw = (Collection<String>) claims.get(tokenService.READ_TENANTS);
            String writeTenant = (String) claims.get(tokenService.WRITE_TENANT);
            Set<String> tenantIds = runningUser.getTenants().parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
            if (writeTenant != null && tenantIds.contains(writeTenant)) {
                Tenant tenant = userrepository.findByIdOrNull(Tenant.class, writeTenant);
                runningUser.setDefaultTenant(tenant);
                runningUser.setImpersonated(true);
            }
            if (readTenantsRaw != null) {
                Set<String> readTenants = readTenantsRaw.parallelStream().collect(Collectors.toSet());
                List<Tenant> tenants = userrepository.findByIds(Tenant.class, readTenants).parallelStream().filter(f -> tenantIds.contains(f.getId())).collect(Collectors.toList());
                runningUser.setTenants(tenants);
                runningUser.setImpersonated(true);
            }
            loggedusers.put(authenticationKey, runningUser);
            return runningUser;
        }


        return null;
    }


    @Override
    public User getUser(String authenticationKey) {
        RunningUser runningUser = getRunningUser(authenticationKey);
        return runningUser != null ? runningUser.getUser() : null;
    }

    @Override
    public User getUserByMail(String mail) {
        return userrepository.findByEmail(mail);
    }


    @Override
    public User getUserByMail(String mail, SecurityContext securityContext) {
        List<User> users = userrepository.findByEmail(mail, securityContext);
        return users.isEmpty() ? null : users.get(0);
    }

    @Override
    public List<Tenant> getUserTenants(String authenticationKey) {
        RunningUser user = getRunningUser(authenticationKey);
        return user.getTenants();
    }


    @Override
    public Tenant getUserDefaultTenant(String authenticationKey) {
        RunningUser user = getRunningUser(authenticationKey);
        return user.getDefaultTenant();
    }

    /**
     * log into the system, if user!=null will not search for it.
     *
     * @param bundle
     * @param user
     * @return
     * @throws UserNotFoundException
     * @throws CheckYourCredentialsException
     */
    @Override
    public RunningUser login(AuthenticationRequestHolder bundle, User user)
            throws UserNotFoundException, CheckYourCredentialsException {
        user = authenticate(bundle, user);
        return registerUserIntoSystem(user);

    }

    @Override
    public RunningUser registerUserIntoSystem(User user) {
        OffsetDateTime expirationDate = OffsetDateTime.now().plusSeconds(Constants.JWTSecondsValid);
        return registerUserIntoSystem(user, expirationDate);
    }


    @Override
    public RunningUser registerUserIntoSystem(User user, OffsetDateTime expirationDate) {
        String jwtToken = tokenService.getJwtToken(user, expirationDate);
        RunningUser runningUser = createRunningUser(user, jwtToken);
        runningUser.setExpiresDate(expirationDate);
        loggedusers.put(jwtToken, runningUser);
        return runningUser;

    }

    /**
     * authenticates a user by email and password or by phonenumber and password or by facebook user id and facebook token
     *
     * @param bundle
     * @param user
     * @return
     */
    @Override
    public User authenticate(AuthenticationRequestHolder bundle, User user) {
        long expiresAt = -1;
        if (user == null) {
            long start = System.currentTimeMillis();
            user = bundle.getMail() != null ? userrepository.findByEmail(bundle.getMail()) : (bundle.getPhoneNumber() != null ? findUserByPhoneNumberOrNull(bundle.getPhoneNumber(), null) : null);
            log.log(Level.INFO, "Time taken to find user by email is: " + (System.currentTimeMillis() - start));
            if (user == null) {
                if (bundle.getMail() != null) {
                    throw new UserNotFoundException("User for email: " + bundle.getMail() + " was not found");

                } else {
                    throw new UserNotFoundException("User for phone number : " + bundle.getPhoneNumber() + " was not found");

                }
            }
            if (bundle.getFacebookUserId() != null && bundle.getFacebookToken() != null) {
                FacebookResponseContainer container = null;//facebookLogin(bundle.getFacebookToken());
                if (container == null || !bundle.getFacebookUserId().equals(container.getData().getUserId()) || !container.getData().isIsValid()) {
                    throw new CheckYourCredentialsException("facebook login failed");
                }
                expiresAt = container.getData().getExpiresAt();

            } else {
                if (isScrypt(user.getPassword())) {
                    if (!SCryptUtil.check(bundle.getPassword(), user.getPassword())) {
                        throw (new CheckYourCredentialsException("Please check your credentials"));
                    }
                } else {
                    if (!MD5Calculator.getMD5(bundle.getPassword()).equals(user.getPassword())) {
                        throw (new CheckYourCredentialsException("Please check your credentials"));
                    } else {
                        user.setdecryptedPassword(bundle.getPassword());
                        userrepository.merge(user);
                    }
                }

            }

        }
        return user;
    }

    @Override
    public User findUserByPhoneNumberOrNull(String phoneNumber, SecurityContext securityContext) {
        List<User> users = userrepository.findUserByPhoneNumber(phoneNumber, securityContext);
        return users.isEmpty() ? null : users.get(0);
    }

    private boolean isScrypt(String password) {
        return password.length() > 40;
    }


    private RunningUser createRunningUser(User user, String jwtKey) {
        RunningUser running = new RunningUser(user, jwtKey);
        List<TenantToUser> tenants = tenantRepository.getAllTenants(user);
        running.setTenants(tenants.parallelStream().map(f -> f.getLeftside()).collect(Collectors.toList()));
        running.setDefaultTenant(tenants.parallelStream().filter(f -> f.isDefualtTennant()).map(f -> f.getLeftside()).findFirst().orElse(null));
        running.setLoggedin(true);

        return running;
    }




    @Override
    public boolean logOut(String authenticationkey) {
        RunningUser runningUser = loggedusers.getIfPresent(authenticationkey);
        if (runningUser != null) {
            if (runningUser.getExpiresDate() == null && OffsetDateTime.now().isBefore(runningUser.getExpiresDate())) {
                blacklist.put(authenticationkey, authenticationkey);
                return true;
            }

        } else {
            JWTClaims claims = tokenService.parseClaimsAndVerifyClaims(authenticationkey, log);
            if (claims != null && (claims.getExpiration() == null || OffsetDateTime.now().isBefore(claims.getExpiration().toInstant().atZone(ZoneId.of("UTC")).toOffsetDateTime()))) {
                blacklist.put(authenticationkey, authenticationkey);
                return true;
            }
        }
        return false;

    }


    public ResetPasswordResponse resetUserPassword(ResetUserPasswordRequest resetUserPasswordRequest, SecurityContext securityContext) {
        User user = resetUserPasswordRequest.getUser();
        user.setdecryptedPassword(resetUserPasswordRequest.getPassword());
        userrepository.merge(user);
        return new ResetPasswordResponse();

    }

    @Override
    public ResetPasswordResponse resetPasswordViaMailPrepare(ResetUserPasswordRequest resetUserPasswordRequest) {
        User user = resetUserPasswordRequest.getUser();
        String verification = getVerificationToken();
        user.setForgotPasswordToken(verification);
        user.setForgotPasswordTokenValid(OffsetDateTime.now().plusMinutes(Constants.verificationLinkValidInMin));
        userrepository.merge(user);
        return new ResetPasswordResponse(verification);

    }

    private String getVerificationToken() {
        return Baseclass.getBase64ID().replaceAll("\\+", "0").replaceAll("/", "1");
    }


    @Override
    public ResetPasswordResponse resetPasswordWithVerification(ResetPasswordWithVerification resetPasswordWithVerification) {
        String verification = resetPasswordWithVerification.getVerification();

        List<User> users = userrepository.getUserByForgotPasswordVerificationToken(verification);
        User user = users.isEmpty() ? null : users.get(0);
        if (user == null) {
            throw new BadRequestCustomException("Invalid token", ResetPasswordResponse.INVALID_TOKEN);
        }
        if (user.getForgotPasswordTokenValid() == null || OffsetDateTime.now().isAfter(user.getForgotPasswordTokenValid())) {
            throw new BadRequestCustomException("Token has expired", ResetPasswordResponse.TOKEN_EXPIRED);
        }
        user.setdecryptedPassword(resetPasswordWithVerification.getPassword());
        user.setForgotPasswordTokenValid(null);
        user.setForgotPasswordToken(null);
        userrepository.merge(user);
        return new ResetPasswordResponse().setEmail(user.getEmail()).setPhoneNumber(user.getEmail());
    }

    @Override
    public VerifyMailResponse verifyMail(VerifyMail verifyMail) {
        String verification = verifyMail.getVerification();

        List<User> users = userrepository.getUserByEmailVerificationToken(verification);
        User user = users.isEmpty() ? null : users.get(0);
        if (user == null) {
            throw new BadRequestCustomException("Invalid token", VerifyMailResponse.INVALID_TOKEN);
        }
        if (user.getEmailVerificationTokenValid() == null || OffsetDateTime.now().isAfter(user.getEmailVerificationTokenValid())) {
            throw new BadRequestCustomException("Token has expired", VerifyMailResponse.TOKEN_EXPIRED);
        }
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenValid(null);
        user.setLastVerificationDate(OffsetDateTime.now());
        userrepository.merge(user);
        return new VerifyMailResponse();

    }


    private VerifyMailResponse verifyMailStartPrepare(VerifyMailStart verifyMail, SecurityContext securityContext) {
        User user = verifyMail.getUser();
        user.setEmailVerificationTokenValid(OffsetDateTime.now().plusMinutes(5));
        String verification = getVerificationToken();
        user.setEmailVerificationToken(verification);
        return new VerifyMailResponse(verification);
    }

    @Override
    public void refrehEntityManager() {

        baselinkRepository.refrehEntityManager();
        tenantRepository.refrehEntityManager();
        userrepository.refrehEntityManager();
    }

    /**
     * @param tenantAdmin
     * @deprecated 2.2.0 replaced by {@link #validateUser(UserCreate, SecurityContext)}
     */
    @Override
    @Deprecated
    public void validateAndpopulateNewUser(NewUser tenantAdmin) {
        populateNewUser(tenantAdmin);
        validateNewUser(tenantAdmin);
    }

    @Override
    public User getAdminUser() {
        return userrepository.findById(Constants.systemAdminId);
    }


    private void validateNewUser(NewUser tenantAdmin) {
        if (tenantAdmin.getEmail() == null && tenantAdmin.getPhonenumber() == null) {
            throw new BadRequestException("email and phone number cant both be null");
        }
        if (tenantAdmin.getApikey() == null) {
            throw new BadRequestException("api key cannot be null");
        }
    }

    private void populateNewUser(NewUser tenantAdmin) {
        if (tenantAdmin.getType() != null) {
            try {
                Class<?> c = Class.forName(tenantAdmin.getType());
                tenantAdmin.setClazz(c);
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE, "no class by name " + tenantAdmin.getType());
                throw new BadRequestException("no type " + tenantAdmin.getType());
            }
        } else {
            tenantAdmin.setClazz(User.class);
        }
    }

    @Override
    public void validateUserForCreate(UserCreate userCreate, SecurityContext securityContext) {
        validateUser(userCreate, securityContext);
        User existing = userCreate.getEmail() != null ? userrepository.findByEmail(userCreate.getEmail()) : findUserByPhoneNumberOrNull(userCreate.getPhoneNumber(), null);
        if (existing != null) {
            String cause = userCreate.getEmail() != null ? "Email" : "PhoneNumber";
            throw new BadRequestException("Cannot create User " + cause + " is not unique");
        }

    }

    @Override
    public void validateUserUpdate(UserUpdate userUpdate, SecurityContext securityContext) {
        baseclassService.validate(userUpdate, securityContext);
        User user = userrepository.getByIdOrNull(userUpdate.getId(), User.class, null, securityContext);
        if (user == null) {
            throw new BadRequestException("No User With id " + userUpdate.getId());
        }
        userUpdate.setUser(user);
        User existing = userUpdate.getEmail() != null ? userrepository.findByEmail(userUpdate.getEmail()) : findUserByPhoneNumberOrNull(userUpdate.getPhoneNumber(), null);
        if (existing != null && !user.getId().equals(existing.getId())) {
            String cause = userUpdate.getEmail() != null ? "Email" : "PhoneNumber";
            throw new BadRequestException("Cannot Update User " + cause + " is not unique");
        }

    }

    @Override
    public void validateUser(UserCreate userCreate, SecurityContext securityContext) {
        baseclassService.validate(userCreate, securityContext);
        Tenant tenant = userCreate.getTenant();
        if (tenant == null) {
            tenant = securityContext.getTenantToCreateIn() != null ? securityContext.getTenantToCreateIn() : (securityContext.getTenants().isEmpty() ? null : securityContext.getTenants().get(0));
        }
        if (tenant == null) {
            throw new BadRequestException("Could not determine tenant to create user in");
        }
        userCreate.setTenant(tenant);
        if (userCreate.getPhoneNumber() == null && userCreate.getEmail() == null) {
            throw new BadRequestException("Phone Number or Email Must Be Provided");
        }


    }

    @Override
    public User updateUser(UserUpdate userUpdate, SecurityContext securityContext) {
        User user = userUpdate.getUser();
        if (updateUserNoMerge(user, userUpdate)) {
            userrepository.merge(user);
        }
        return user;
    }

    @Override
    public PaginationResponse<User> getAllUsers(UserFiltering userFiltering, SecurityContext securityContext) {
        List<User> list = listAllUsers(userFiltering, securityContext);
        long count = userrepository.countAllUsers(userFiltering, securityContext);
        return new PaginationResponse<>(list, userFiltering, count);
    }

    @Override
    public List<User> listAllUsers(UserFiltering userFiltering, SecurityContext securityContext) {
        return userrepository.getAllUsers(userFiltering, securityContext);
    }

    @Override
    public List<TenantToUser> getAllTenantToUsers(TenantToUserFilter userFiltering, SecurityContext securityContext) {
        return userrepository.getAllTenantToUsers(userFiltering, securityContext);
    }

    @Override
    public RoleToUser createRoleToUserNoMerge(RoleToUserCreate roleToUserCreate, SecurityContext securityContext) {
        RoleToUser roleToUser = new RoleToUser("roleLink", securityContext);
        roleToUser.setRole(roleToUserCreate.getRole());
        roleToUser.setUser(roleToUserCreate.getUser());
        return roleToUser;
    }

    public boolean updateRoleToUserNoMerge(RoleToUserCreate roleToUserCreate, RoleToUser roleToUser) {
        boolean update=baseclassService.updateBaseclassNoMerge(roleToUserCreate,roleToUser);
        if(roleToUserCreate.getRole()!=null && (roleToUser.getLeftside()==null || !roleToUserCreate.getRole().getId().equals(roleToUser.getLeftside().getId()))){
            roleToUser.setRole(roleToUserCreate.getRole());
            update=true;
        }
        if(roleToUserCreate.getUser()!=null && (roleToUser.getRightside()==null || !roleToUserCreate.getUser().getId().equals(roleToUser.getRightside().getId()))){
            roleToUser.setUser(roleToUserCreate.getUser());
            update=true;
        }
        return update;
    }

    @Override
    public UserProfile getUserProfile(UserProfileRequest userProfileRequest, SecurityContext securityContext) {
        Tenant tenantToCreateIn = securityContext.getTenantToCreateIn();
        Tenant tenant = tenantToCreateIn == null && !securityContext.getTenants().isEmpty() ? securityContext.getTenants().get(0) : tenantToCreateIn;
        return new UserProfile().setUser(securityContext.getUser()).setTenant(tenant);
    }

    @Override
    public void validate(UserFiltering userFiltering, SecurityContext securityContext) {

        Set<String> tenantIds = userFiltering.getUserTenantsIds();
        Map<String, Tenant> map = tenantIds.isEmpty() ? new HashMap<>() : userrepository.listByIds(Tenant.class, tenantIds, securityContext).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        tenantIds.removeAll(map.keySet());
        if (!tenantIds.isEmpty()) {
            throw new BadRequestException("No Tenant with ids " + tenantIds);
        }
        userFiltering.setUserTenants(new ArrayList<>(map.values()));

    }

    @Override
    public void validate(AuthenticationRequest authenticationRequest, SecurityContext securityContext) {

        if (authenticationRequest.getEmail() == null && authenticationRequest.getPhoneNumber() == null) {
            throw new BadRequestCustomException("Email Or Phone Must be Provided", LoginErrors.IDENTIFIER_NOT_PROVIDED.getCode());
        }
        if (authenticationRequest.getPassword() == null) {
            throw new BadRequestCustomException("Password must be provided", LoginErrors.PASSWORD_NOT_PROVIDED.getCode());
        }
        UserFiltering userFiltering = new UserFiltering();
        if (authenticationRequest.getEmail() != null) {
            userFiltering.setEmails(Collections.singleton(authenticationRequest.getEmail()));
        } else {
            if (authenticationRequest.getPhoneNumber() != null) {
                userFiltering.setPhoneNumbers(Collections.singleton(authenticationRequest.getPhoneNumber()));
            }
        }


        List<User> user = listAllUsers(userFiltering, securityContext);
        if (user.isEmpty()) {
            throw new NotAuthorizedException("User Not Authorized");
        }
        authenticationRequest.setUser(user.get(0));
    }

    @Override
    public AuthenticationResponse login(AuthenticationRequest authenticationRequest, SecurityContext securityContext) {
        User user = authenticationRequest.getUser();
        if (!SCryptUtil.check(authenticationRequest.getPassword(), user.getPassword())) {
            throw (new CheckYourCredentialsException("Please check your credentials"));
        }
        OffsetDateTime expirationDate = OffsetDateTime.now().plusSeconds(authenticationRequest.getSecondsValid() != 0 ? authenticationRequest.getSecondsValid() : Constants.JWTSecondsValid);
        String jwtToken = tokenService.getJwtToken(user, expirationDate);
        return new AuthenticationResponse().setAuthenticationKey(jwtToken).setTokenExpirationDate(expirationDate).setUserId(user.getId());

    }


    @Override
    public void validate(ImpersonateRequest impersonateRequest, SecurityContext securityContext) {

        String creationTenantId = impersonateRequest.getCreationTenantId();
        Tenant creationTenant = userrepository.getByIdOrNull(creationTenantId, Tenant.class, null, securityContext);
        if (creationTenant == null) {
            throw new BadRequestException("no tenant with id " + creationTenantId);
        }
        impersonateRequest.setCreationTenant(creationTenant);

        Set<String> readTenantIds = impersonateRequest.getReadTenantsIds();
        Map<String, Tenant> tenantMap = readTenantIds.isEmpty() ? new HashMap<>() : userrepository.listByIds(Tenant.class, readTenantIds, securityContext).parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f));
        readTenantIds.removeAll(tenantMap.keySet());
        if (!readTenantIds.isEmpty()) {
            throw new BadRequestException("No Tenants with ids " + readTenantIds);
        }
        impersonateRequest.setReadTenants(new ArrayList<>(tenantMap.values()));
    }

    @Override
    public ImpersonateResponse impersonate(ImpersonateRequest impersonateRequest, SecurityContext securityContext) {
        User user = securityContext.getUser();
        OffsetDateTime expirationDate = OffsetDateTime.now().plusSeconds(Constants.JWTSecondsValid);
        String writeTenant = impersonateRequest.getCreationTenant().getId();
        Set<String> readTenants = impersonateRequest.getReadTenants().parallelStream().map(f -> f.getId()).collect(Collectors.toSet());
        String jwtToken = tokenService.getJwtToken(user, expirationDate, writeTenant, readTenants);
        return new ImpersonateResponse().setAuthenticationKey(jwtToken);

    }

    public List<RoleToUser> listAllRoleToUsers(RoleToUserFilter roleToUserFilter, SecurityContext securityContext) {
        return userrepository.listAllRoleToUsers(roleToUserFilter,securityContext);

    }
}
