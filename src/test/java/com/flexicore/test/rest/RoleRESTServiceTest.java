package com.flexicore.test.rest;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.Role;
import com.flexicore.request.AuthenticationRequest;
import com.flexicore.request.RoleCreate;
import com.flexicore.request.RoleFilter;
import com.flexicore.request.RoleUpdate;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FlexiCoreApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")

public class RoleRESTServiceTest {

    private Role role;
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
    public void testRoleCreate() {
        String name = UUID.randomUUID().toString();
        RoleCreate request = new RoleCreate()
                .setName(name);
        ResponseEntity<Role> roleResponse = this.restTemplate.postForEntity("/FlexiCore/rest/roles/createRole", request, Role.class);
        Assertions.assertEquals(200, roleResponse.getStatusCodeValue());
        role = roleResponse.getBody();
        assertRole(request, role);

    }

    @Test
    @Order(2)
    public void testListAllRoles() {
        RoleFilter request=new RoleFilter()
                .setNames(Collections.singleton(role.getName()));
        ParameterizedTypeReference<PaginationResponse<Role>> t=new ParameterizedTypeReference<PaginationResponse<Role>>() {};

        ResponseEntity<PaginationResponse<Role>> roleResponse = this.restTemplate.exchange("/FlexiCore/rest/roles/getAllRoles", HttpMethod.POST, new HttpEntity<>(request), t);
        Assertions.assertEquals(200, roleResponse.getStatusCodeValue());
        PaginationResponse<Role> body = roleResponse.getBody();
        Assertions.assertNotNull(body);
        List<Role> roles = body.getList();
        Assertions.assertNotEquals(0,roles.size());
        Assertions.assertTrue(roles.stream().anyMatch(f->f.getId().equals(role.getId())));


    }

    public void assertRole(RoleCreate request, Role role) {
        Assertions.assertNotNull(role);
        Assertions.assertEquals(request.getName(), role.getName());
    }

    @Test
    @Order(3)
    public void testRoleUpdate(){
        String name = UUID.randomUUID().toString();
        RoleUpdate request = new RoleUpdate()
                .setId(role.getId())
                .setName(name);
        ResponseEntity<Role> roleResponse = this.restTemplate.exchange("/FlexiCore/rest/roles/updateRole",HttpMethod.PUT, new HttpEntity<>(request), Role.class);
        Assertions.assertEquals(200, roleResponse.getStatusCodeValue());
        role = roleResponse.getBody();
        assertRole(request, role);

    }

}
