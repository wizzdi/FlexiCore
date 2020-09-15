package com.flexicore.rest;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.Category;
import com.flexicore.model.Role;
import com.flexicore.request.*;
import com.flexicore.response.AuthenticationResponse;
import com.flexicore.response.MassDeleteResponse;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FlexiCoreApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")

public class BaseclassRESTServiceTest {

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

    private Category createCategory() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("categoryName",System.currentTimeMillis()+"");
        HttpEntity<?> request=new HttpEntity<>(null,headers);
        ResponseEntity<Category> createCategory = this.restTemplate.exchange("/FlexiCore/rest/category",HttpMethod.POST, request, Category.class);
        return createCategory.getBody();



    }

    @Test
    @Order(1)
    public void testMassDelete() {
        String name = UUID.randomUUID().toString();
        Category category = createCategory();
        MassDeleteRequest request = new MassDeleteRequest()
                .setIds(Collections.singleton(category.getId()));
        ResponseEntity<MassDeleteResponse> roleResponse = this.restTemplate.postForEntity("/FlexiCore/rest/baseclass/massDelete", request, MassDeleteResponse.class);
        Assertions.assertEquals(200, roleResponse.getStatusCodeValue());
        MassDeleteResponse body = roleResponse.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(body.getDeletedIds(),request.getIds());
        ResponseEntity<Category> categoryResponse = this.restTemplate.getForEntity("/FlexiCore/rest/baseclass/getbyid/"+category.getId()+"/"+Category.class.getCanonicalName(), Category.class);
        Assertions.assertEquals(400,categoryResponse.getStatusCodeValue());

       roleResponse = this.restTemplate.postForEntity("/FlexiCore/rest/baseclass/massDelete", request, MassDeleteResponse.class);
        Assertions.assertEquals(200, roleResponse.getStatusCodeValue());
        body = roleResponse.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.getDeletedIds().isEmpty());


    }


}
