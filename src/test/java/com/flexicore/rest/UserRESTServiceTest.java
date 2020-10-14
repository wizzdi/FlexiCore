package com.flexicore.rest;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.rest.All;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.*;
import com.flexicore.request.*;
import com.flexicore.response.AuthenticationResponse;
import com.flexicore.response.ImpersonateResponse;
import com.flexicore.response.UserProfile;
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

import java.util.*;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FlexiCoreApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class UserRESTServiceTest {

    private String password;
    private User user;
    private String authenticationKey;
    @Autowired
    private TestRestTemplate restTemplate;


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

    @Test
    @Order(1)
    public void testUserCreate() {
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
        assertUser(request, user);

    }

    @Test
    @Order(2)
    public void testListAllUsers() {
        UserFiltering request=new UserFiltering()
                .setEmails(Collections.singleton(user.getEmail()));
        ParameterizedTypeReference<PaginationResponse<User>> t=new ParameterizedTypeReference<PaginationResponse<User>>() {};

        ResponseEntity<PaginationResponse<User>> userResponse = this.restTemplate.exchange("/FlexiCore/rest/users/getAllUsers", HttpMethod.POST, new HttpEntity<>(request), t);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        PaginationResponse<User> body = userResponse.getBody();
        Assertions.assertNotNull(body);
        List<User> users = body.getList();
        Assertions.assertNotEquals(0,users.size());
        Assertions.assertTrue(users.stream().anyMatch(f->f.getId().equals(user.getId())));


    }

    public void assertUser(UserCreate request, User user) {
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
    }

    @Test
    @Order(3)
    public void testUserUpdate(){
        String name = UUID.randomUUID().toString();
        String email = name + "@test.com";
        String lastName = name + "_Last";
        password = name + "_pass";
        String phoneNumber = name + "_phone";
        UserUpdate request = new UserUpdate()
                .setId(user.getId())
                .setEmail(email)
                .setLastName(lastName)
                .setPassword(password)
                .setPhoneNumber(phoneNumber)
                .setName(name);
        ResponseEntity<User> userResponse = this.restTemplate.exchange("/FlexiCore/rest/users/updateUser",HttpMethod.PUT, new HttpEntity<>(request), User.class);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        user = userResponse.getBody();
        assertUser(request, user);

    }


    public Tenant createTenantAdmin(String name,String email,String password) {
        UserCreate userCreate=new UserCreate()
                .setPassword(password)
                .setEmail(email)
                .setPhoneNumber(name+"_phone")
                .setName(name+"_user");
        TenantCreate request = new TenantCreate()
                .setTenantAdmin(userCreate)
                .setName(name);
        ResponseEntity<Tenant> tenantResponse = this.restTemplate.postForEntity("/FlexiCore/rest/tenant/createTenant", request, Tenant.class);
        return tenantResponse.getBody();


    }
private String login(String email,String password){
    AuthenticationRequest authenticationRequest = new AuthenticationRequest()
            .setEmail(email)
            .setPassword(password);
    ResponseEntity<AuthenticationResponse> authenticationResponse = this.restTemplate.postForEntity("/FlexiCore/rest/authenticationNew/login", authenticationRequest, AuthenticationResponse.class);
    AuthenticationResponse authenticationResponseBody = authenticationResponse.getBody();
    Assertions.assertNotNull(authenticationResponseBody);
    return authenticationResponseBody.getAuthenticationKey();
}

    @Test
    @Order(4)
    public void testImpersonate() throws InterruptedException {
        String name = UUID.randomUUID().toString();

        String email = "admin@" + name + ".com";
        String password = name + "_pass";
        Tenant tenant=createTenantAdmin(name, email, password);


        String adminToken=this.authenticationKey;
        String userToken=login(email,password);
        this.authenticationKey=userToken;
        ResponseEntity<UserProfile> userProfileResponse = this.restTemplate.exchange("/FlexiCore/rest/users/getUserProfile",HttpMethod.POST, new HttpEntity<>(new UserProfileRequest()), UserProfile.class);
        UserProfile userProfile = userProfileResponse.getBody();
        Assertions.assertNotNull(userProfile);
        User user = userProfile.getUser();

        Tenant defaultTenant = this.user.getTenant();
        this.authenticationKey=adminToken;

        BaselinkMassCreate request = new BaselinkMassCreate()
                .setLeftsideIds(Collections.singleton(defaultTenant.getId()))
                .setRightsideIds(Collections.singleton(user.getId()))
                .setLinkClassName(TenantToUser.class.getCanonicalName());
        createBaselink(request);
        request = new BaselinkMassCreate()
                .setLeftsideIds(Collections.singleton(defaultTenant.getId()))
                .setRightsideIds(Collections.singleton(defaultTenant.getId()))
                .setValueId(Baseclass.generateUUIDFromString(All.class.getCanonicalName()))
                .setSimpleValue(IOperation.Access.allow.name())
                .setLinkClassName(TenantToBaseClassPremission.class.getCanonicalName());
        createBaselink(request);
        Thread.sleep(5000);
        userToken=login(email,password);
        this.authenticationKey=userToken;
        userProfileResponse = this.restTemplate.exchange("/FlexiCore/rest/users/getUserProfile",HttpMethod.POST, new HttpEntity<>(new UserProfileRequest()), UserProfile.class);
        userProfile = userProfileResponse.getBody();
        Assertions.assertNotNull(userProfile);
        Assertions.assertEquals(userProfile.getTenants().stream().map(f->f.getId()).collect(Collectors.toSet()),new HashSet<>(Arrays.asList(defaultTenant.getId(),tenant.getId())));

        User categoryBeforeImpersonate=createUser();
        Assertions.assertEquals(tenant.getId(),categoryBeforeImpersonate.getTenant().getId());

        ImpersonateRequest impersonateRequest=new ImpersonateRequest()
                .setCreationTenantId(defaultTenant.getId())
                .setReadTenantsIds(Collections.singleton(defaultTenant.getId()));

        ResponseEntity<ImpersonateResponse> impersonateResponseResponseEntity = this.restTemplate.exchange("/FlexiCore/rest/users/impersonate",HttpMethod.POST, new HttpEntity<>(impersonateRequest), ImpersonateResponse.class);
        Assertions.assertEquals(200, impersonateResponseResponseEntity.getStatusCodeValue());
        ImpersonateResponse impersonateResponse = impersonateResponseResponseEntity.getBody();
        Assertions.assertNotNull(impersonateResponse);
        this.authenticationKey=impersonateResponse.getAuthenticationKey();
        User categoryAfterImpersonate=createUser();
        Assertions.assertEquals(defaultTenant.getId(),categoryAfterImpersonate.getTenant().getId());


    }

    private void createBaselink(BaselinkMassCreate request) {
        ParameterizedTypeReference<List<Baselink>> t=new ParameterizedTypeReference<List<Baselink>>() {};
        ResponseEntity<List<Baselink>> tenantLinkResponse = this.restTemplate.exchange("/FlexiCore/rest/baselinks/massCreateBaselink",HttpMethod.POST,new HttpEntity<>(request), t);
        Assertions.assertNotNull(tenantLinkResponse.getBody());
    }

    private User createUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("categoryName",System.currentTimeMillis()+"");
        HttpEntity<?> request=new HttpEntity<>(new UserCreate().setPassword("test").setEmail(UUID.randomUUID().toString()+"@test.com").setName("test"),headers);
        ResponseEntity<User> createCategory = this.restTemplate.exchange("/FlexiCore/rest/users/createUser",HttpMethod.POST, request, User.class);
        return createCategory.getBody();



    }


}
