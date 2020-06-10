package com.flexicore.rest;

import com.flexicore.data.jsoncontainers.CreatePermissionGroupRequest;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.PermissionGroup;
import com.flexicore.request.AuthenticationRequest;
import com.flexicore.request.PermissionGroupsFilter;
import com.flexicore.request.UpdatePermissionGroup;
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

public class PermissionGroupRESTServiceTest {

    private PermissionGroup permissionGroup;
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
    public void testCreatePermissionGroupRequest() {
        String name = UUID.randomUUID().toString();
        CreatePermissionGroupRequest request = new CreatePermissionGroupRequest()
                .setName(name);
        ResponseEntity<PermissionGroup> permissionGroupResponse = this.restTemplate.postForEntity("/FlexiCore/rest/permissionGroup/createPermissionGroup", request, PermissionGroup.class);
        Assertions.assertEquals(200, permissionGroupResponse.getStatusCodeValue());
        permissionGroup = permissionGroupResponse.getBody();
        assertPermissionGroup(request, permissionGroup);

    }

    @Test
    @Order(2)
    public void testListAllPermissionGroups() {
        PermissionGroupsFilter request=new PermissionGroupsFilter();
        request.setNameLike(permissionGroup.getName());
        ParameterizedTypeReference<PaginationResponse<PermissionGroup>> t=new ParameterizedTypeReference<PaginationResponse<PermissionGroup>>() {};

        ResponseEntity<PaginationResponse<PermissionGroup>> permissionGroupResponse = this.restTemplate.exchange("/FlexiCore/rest/permissionGroup/getAllPermissionGroups", HttpMethod.POST, new HttpEntity<>(request), t);
        Assertions.assertEquals(200, permissionGroupResponse.getStatusCodeValue());
        PaginationResponse<PermissionGroup> body = permissionGroupResponse.getBody();
        Assertions.assertNotNull(body);
        List<PermissionGroup> permissionGroups = body.getList();
        Assertions.assertNotEquals(0,permissionGroups.size());
        Assertions.assertTrue(permissionGroups.stream().anyMatch(f->f.getId().equals(permissionGroup.getId())));


    }

    public void assertPermissionGroup(CreatePermissionGroupRequest request, PermissionGroup permissionGroup) {
        Assertions.assertNotNull(permissionGroup);
        Assertions.assertEquals(request.getName(), permissionGroup.getName());
    }

    @Test
    @Order(3)
    public void testUpdatePermissionGroup(){
        String name = UUID.randomUUID().toString();
        UpdatePermissionGroup request = new UpdatePermissionGroup()
                .setId(permissionGroup.getId());
        request
                .setName(name);
        ResponseEntity<PermissionGroup> permissionGroupResponse = this.restTemplate.postForEntity("/FlexiCore/rest/permissionGroup/updatePermissionGroup", request, PermissionGroup.class);
        Assertions.assertEquals(200, permissionGroupResponse.getStatusCodeValue());
        permissionGroup = permissionGroupResponse.getBody();
        assertPermissionGroup(request, permissionGroup);

    }

}
