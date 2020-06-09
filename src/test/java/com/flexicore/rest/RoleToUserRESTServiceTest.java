package com.flexicore.rest;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.Role;
import com.flexicore.model.RoleToUser;
import com.flexicore.model.User;
import com.flexicore.request.*;
import com.flexicore.response.AuthenticationResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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

public class RoleToUserRESTServiceTest {

    private RoleToUser roleToUser;
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
    public void testRoleToUserCreate() {
        String name = UUID.randomUUID().toString();
        String email = name + "@test.com";
        String lastName = name + "_Last";
        String password = name + "_pass";
        String phoneNumber = name + "_phone";
        UserCreate userCreate = new UserCreate()
                .setEmail(email)
                .setLastName(lastName)
                .setPassword(password)
                .setPhoneNumber(phoneNumber)
                .setName(name);
        ResponseEntity<User> userResponse = this.restTemplate.postForEntity("/FlexiCore/rest/users/createUser", userCreate, User.class);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        User user = userResponse.getBody();
        Assertions.assertNotNull(user);
        RoleCreate roleCreate = new RoleCreate()
                .setName(name);
        ResponseEntity<Role> roleResponse = this.restTemplate.postForEntity("/FlexiCore/rest/roles/createRole", roleCreate, Role.class);
        Assertions.assertEquals(200, roleResponse.getStatusCodeValue());
        Role role = roleResponse.getBody();
        Assertions.assertNotNull(role);

        RoleToUserCreate request = new RoleToUserCreate()
                .setRoleId(role.getId())
                .setUserId(user.getId())
                .setName(name);
        ResponseEntity<RoleToUser> roleToUserResponse = this.restTemplate.postForEntity("/FlexiCore/rest/roleToUsers/createRoleToUser", request, RoleToUser.class);
        Assertions.assertEquals(200, roleToUserResponse.getStatusCodeValue());
        roleToUser = roleToUserResponse.getBody();
        assertRoleToUser(request, roleToUser);

    }

    @Test
    @Order(2)
    public void testListAllRoleToUsers() {
        RoleToUserFilter request=new RoleToUserFilter()
                .setRolesIds(Collections.singleton(roleToUser.getLeftside().getId()))
                .setUsersIds(Collections.singleton(roleToUser.getRightside().getId()));
        ParameterizedTypeReference<PaginationResponse<RoleToUser>> t=new ParameterizedTypeReference<PaginationResponse<RoleToUser>>() {};

        ResponseEntity<PaginationResponse<RoleToUser>> roleToUserResponse = this.restTemplate.exchange("/FlexiCore/rest/roleToUsers/getAllRoleToUsers", HttpMethod.POST, new HttpEntity<>(request), t);
        Assertions.assertEquals(200, roleToUserResponse.getStatusCodeValue());
        PaginationResponse<RoleToUser> body = roleToUserResponse.getBody();
        Assertions.assertNotNull(body);
        List<RoleToUser> roleToUsers = body.getList();
        Assertions.assertNotEquals(0,roleToUsers.size());
        Assertions.assertTrue(roleToUsers.stream().anyMatch(f->f.getId().equals(roleToUser.getId())));


    }

    public void assertRoleToUser(RoleToUserCreate request, RoleToUser roleToUser) {
        Assertions.assertNotNull(roleToUser);
        Assertions.assertEquals(request.getName(), roleToUser.getName());
    }

    @Test
    @Order(3)
    public void testRoleToUserUpdate(){
        String name = UUID.randomUUID().toString();
        RoleToUserUpdate request = new RoleToUserUpdate()
                .setId(roleToUser.getId())
                .setName(name);
        ResponseEntity<RoleToUser> roleToUserResponse = this.restTemplate.exchange("/FlexiCore/rest/roleToUsers/updateRoleToUser",HttpMethod.PUT, new HttpEntity<>(request), RoleToUser.class);
        Assertions.assertEquals(200, roleToUserResponse.getStatusCodeValue());
        roleToUser = roleToUserResponse.getBody();
        assertRoleToUser(request, roleToUser);

    }

}
