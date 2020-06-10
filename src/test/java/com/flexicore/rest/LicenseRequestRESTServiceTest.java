package com.flexicore.rest;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.Tenant;
import com.flexicore.model.licensing.LicenseRequest;
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

public class LicenseRequestRESTServiceTest {

    private LicenseRequest licenseRequest;
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
    public void testLicenseRequestCreate() {
        String name = UUID.randomUUID().toString();
        ParameterizedTypeReference<PaginationResponse<Tenant>> t=new ParameterizedTypeReference<PaginationResponse<Tenant>>() {};

        ResponseEntity<PaginationResponse<Tenant>> tenantResponse = this.restTemplate.exchange("/FlexiCore/rest/tenant/getAllTenants", HttpMethod.POST, new HttpEntity<>(new TenantFilter()), t);
        Assertions.assertEquals(200, tenantResponse.getStatusCodeValue());
        PaginationResponse<Tenant> body = tenantResponse.getBody();
        Assertions.assertNotNull(body);
        List<Tenant> tenants = body.getList();
        Assertions.assertFalse(tenants.isEmpty());
        LicenseRequestCreate request = new LicenseRequestCreate()
                .setLicensedTenantId(tenants.get(0).getId())
                .setName(name);
        ResponseEntity<LicenseRequest> licenseRequestResponse = this.restTemplate.postForEntity("/FlexiCore/rest/licenseRequests/createLicenseRequest", request, LicenseRequest.class);
        Assertions.assertEquals(200, licenseRequestResponse.getStatusCodeValue());
        licenseRequest = licenseRequestResponse.getBody();
        assertLicenseRequest(request, licenseRequest);

    }

    @Test
    @Order(2)
    public void testListAllLicenseRequests() {
        LicenseRequestFiltering request = new LicenseRequestFiltering();
        request.setNameLike(licenseRequest.getName());
        ParameterizedTypeReference<PaginationResponse<LicenseRequest>> t = new ParameterizedTypeReference<PaginationResponse<LicenseRequest>>() {
        };

        ResponseEntity<PaginationResponse<LicenseRequest>> licenseRequestResponse = this.restTemplate.exchange("/FlexiCore/rest/licenseRequests/getAllLicenseRequests", HttpMethod.POST, new HttpEntity<>(request), t);
        Assertions.assertEquals(200, licenseRequestResponse.getStatusCodeValue());
        PaginationResponse<LicenseRequest> body = licenseRequestResponse.getBody();
        Assertions.assertNotNull(body);
        List<LicenseRequest> licenseRequests = body.getList();
        Assertions.assertNotEquals(0, licenseRequests.size());
        Assertions.assertTrue(licenseRequests.stream().anyMatch(f -> f.getId().equals(licenseRequest.getId())));


    }

    public void assertLicenseRequest(LicenseRequestCreate request, LicenseRequest licenseRequest) {
        Assertions.assertNotNull(licenseRequest);
        Assertions.assertEquals(request.getName(), licenseRequest.getName());

    }

    @Test
    @Order(3)
    public void testLicenseRequestUpdate() {
        String name = UUID.randomUUID().toString();
        LicenseRequestUpdate request = new LicenseRequestUpdate()
                .setId(licenseRequest.getId())
                .setName(name);
        ResponseEntity<LicenseRequest> licenseRequestResponse = this.restTemplate.exchange("/FlexiCore/rest/licenseRequests/updateLicenseRequest", HttpMethod.PUT, new HttpEntity<>(request), LicenseRequest.class);
        Assertions.assertEquals(200, licenseRequestResponse.getStatusCodeValue());
        licenseRequest = licenseRequestResponse.getBody();
        assertLicenseRequest(request, licenseRequest);

    }

}
