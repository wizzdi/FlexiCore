package com.flexicore.test.rest;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.rest.All;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.*;
import com.flexicore.request.*;
import com.flexicore.response.*;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.TimeProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FlexiCoreApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class TotpRESTServiceTest {

    private String password;
    private User user;
    private String authenticationKey;
    private String preTotpKey;
    @Autowired
    private TestRestTemplate restTemplate;
    private String secret;
    private List<String> recoveryCodes;
    @Autowired
    private CodeGenerator codeGenerator;
    @Autowired
    private TimeProvider timeProvider;


    @BeforeAll
    private void init() {
        ResponseEntity<AuthenticationResponse> authenticationResponse = this.restTemplate.postForEntity("/FlexiCore/rest/authenticationNew/login", new AuthenticationRequest().setEmail("admin@flexicore.com").setPassword("admin"), AuthenticationResponse.class);
        this.authenticationKey= authenticationResponse.getBody().getAuthenticationKey();
        restTemplate.getRestTemplate().setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add("authenticationKey", authenticationKey);
                    return execution.execute(request, body);
                }));
    }


    public void userCreate() {
        String name = UUID.randomUUID().toString();
        String email = name + "@test.com";
        String lastName = name + "_Last";
        password = name + "_pass";
        String phoneNumber = name + "_phone";
        UserCreate request = new UserCreate()
                .setEmail(email)
                .setLastName(lastName)
                .setPassword(password)
                .setPhoneNumber(phoneNumber)
                .setName(name);
        ResponseEntity<User> userResponse = this.restTemplate.postForEntity("/FlexiCore/rest/users/createUser", request, User.class);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        user = userResponse.getBody();
        preTotpKey =assertUser(request, user);
        authenticationKey=preTotpKey;

    }


    @Test
    @Order(1)
    public void testSetupTotp() {
        userCreate();
        ParameterizedTypeReference<SetupTotpResponse> t=new ParameterizedTypeReference<>() {};

        ResponseEntity<SetupTotpResponse> userResponse = this.restTemplate.exchange("/FlexiCore/rest/totp/setupTotp", HttpMethod.POST, new HttpEntity<>(null), t);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        SetupTotpResponse body = userResponse.getBody();
        Assertions.assertNotNull(body);
        secret=body.getSecret();


    }

    @Test
    @Order(2)
    public void testFinishSetupTotp() throws CodeGenerationException {

        ParameterizedTypeReference<FinishTotpSetupResponse> t=new ParameterizedTypeReference<>() {};
        long currentBucket = Math.floorDiv(this.timeProvider.getTime(),30);

        String code = codeGenerator.generate(secret, currentBucket);

        FinishTotpSetupRequest headers = new FinishTotpSetupRequest().setCode(code);
        ResponseEntity<FinishTotpSetupResponse> userResponse = this.restTemplate.exchange("/FlexiCore/rest/totp/finishSetupTotp", HttpMethod.POST, new HttpEntity<>(headers), t);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        FinishTotpSetupResponse body = userResponse.getBody();
        Assertions.assertNotNull(body);
        recoveryCodes=body.getTotpRecoveryCodes();


    }

    public String assertUser(UserCreate request, User user) {
        Assertions.assertNotNull(user);
        Assertions.assertEquals(request.getName(), user.getName());
        Assertions.assertEquals(request.getLastName(), user.getSurName());
        Assertions.assertEquals(request.getPhoneNumber(), user.getPhoneNumber());
        ResponseEntity<AuthenticationResponse> authenticationResponse = this.restTemplate.postForEntity("/FlexiCore/rest/authenticationNew/login", new AuthenticationRequest().setEmail(request.getEmail()).setPassword(request.getPassword()), AuthenticationResponse.class);
        Assertions.assertEquals(200, authenticationResponse.getStatusCodeValue());
        AuthenticationResponse body = authenticationResponse.getBody();
        Assertions.assertNotNull(body);
        String userToken = body.getAuthenticationKey();
        Assertions.assertNotNull(userToken);
        return userToken;
    }

    @Test
    @Order(3)
    public void testTotpEnforces2F(){
        PermissionGroupsFilter request=new PermissionGroupsFilter();
        ParameterizedTypeReference<PaginationResponse<PermissionGroup>> t= new ParameterizedTypeReference<>() {};

        ResponseEntity<PaginationResponse<PermissionGroup>> permissionGroupResponse = this.restTemplate.exchange("/FlexiCore/rest/permissionGroup/getAllPermissionGroups", HttpMethod.POST, new HttpEntity<>(request), t);
        Assertions.assertEquals(401,permissionGroupResponse.getStatusCodeValue());

    }

    @Test
    @Order(4)
    public void testAuthenticateTotp() throws CodeGenerationException {
        long currentBucket = Math.floorDiv(this.timeProvider.getTime(),30);

        String code = codeGenerator.generate(secret, currentBucket);
        ParameterizedTypeReference<TotpAuthenticationResponse> t=new ParameterizedTypeReference<>() {};

        TotpAuthenticationRequest totpAuthenticationRequest = new TotpAuthenticationRequest().setCode(code);
        ResponseEntity<TotpAuthenticationResponse> userResponse = this.restTemplate.exchange("/FlexiCore/rest/totp/authenticateTotp", HttpMethod.POST, new HttpEntity<>(totpAuthenticationRequest), t);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        TotpAuthenticationResponse body = userResponse.getBody();
        Assertions.assertNotNull(body);
        authenticationKey=body.getTotpAuthenticationToken();
        PermissionGroupsFilter request=new PermissionGroupsFilter();
        ParameterizedTypeReference<PaginationResponse<PermissionGroup>> pt= new ParameterizedTypeReference<>() {};

        ResponseEntity<PaginationResponse<PermissionGroup>> permissionGroupResponse = this.restTemplate.exchange("/FlexiCore/rest/permissionGroup/getAllPermissionGroups", HttpMethod.POST, new HttpEntity<>(request), pt);
        Assertions.assertEquals(200,permissionGroupResponse.getStatusCodeValue());


    }

    @Test
    @Order(5)
    public void testRecoveryCodes() throws CodeGenerationException {
        authenticationKey=preTotpKey;
        String recoveryCode=recoveryCodes.get(0);
        ParameterizedTypeReference<TotpAuthenticationResponse> t=new ParameterizedTypeReference<>() {};

        RecoverTotpRequest recoverTotpRequest = new RecoverTotpRequest().setRecoveryCode(recoveryCode);
        ResponseEntity<TotpAuthenticationResponse> userResponse = this.restTemplate.exchange("/FlexiCore/rest/totp/recoverTotp", HttpMethod.POST, new HttpEntity<>(recoverTotpRequest), t);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        TotpAuthenticationResponse body = userResponse.getBody();
        Assertions.assertNotNull(body);
        authenticationKey=body.getTotpAuthenticationToken();
        PermissionGroupsFilter request=new PermissionGroupsFilter();
        ParameterizedTypeReference<PaginationResponse<PermissionGroup>> pt= new ParameterizedTypeReference<>() {};

        ResponseEntity<PaginationResponse<PermissionGroup>> permissionGroupResponse = this.restTemplate.exchange("/FlexiCore/rest/permissionGroup/getAllPermissionGroups", HttpMethod.POST, new HttpEntity<>(request), pt);
        Assertions.assertEquals(200,permissionGroupResponse.getStatusCodeValue());

        ParameterizedTypeReference<TotpAuthenticationResponse> rt=new ParameterizedTypeReference<>() {};

        ResponseEntity<TotpAuthenticationResponse> totpAuthenticationResponseResponseEntity = this.restTemplate.exchange("/FlexiCore/rest/totp/recoverTotp", HttpMethod.POST, new HttpEntity<>(recoverTotpRequest), rt);
        Assertions.assertEquals(401, totpAuthenticationResponseResponseEntity.getStatusCodeValue());


    }


}
