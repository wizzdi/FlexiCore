package com.flexicore.rest;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.User;
import com.flexicore.request.*;
import com.flexicore.response.AuthenticationResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FlexiCoreApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Profile("test")
public class UserRESTServiceTest {

    private String password;
    private User user;
    private String userToken;
    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    private void init() {
        ResponseEntity<AuthenticationResponse> authenticationResponse = this.restTemplate.postForEntity("/FlexiCore/rest/authenticationNew/login", new AuthenticationRequest().setEmail("admin@flexicore.com").setPassword("admin"), AuthenticationResponse.class);
        String authenticationKey = authenticationResponse.getBody().getAuthenticationKey();
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
        userToken = body.getAuthenticationKey();
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

}
