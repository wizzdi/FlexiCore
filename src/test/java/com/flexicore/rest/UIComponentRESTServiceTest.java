package com.flexicore.rest;

import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.data.jsoncontainers.UIComponentRegistrationContainer;
import com.flexicore.data.jsoncontainers.UIComponentsRegistrationContainer;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.PermissionGroup;
import com.flexicore.model.User;
import com.flexicore.model.ui.UIComponent;
import com.flexicore.request.AuthenticationRequest;
import com.flexicore.request.PermissionGroupsFilter;
import com.flexicore.request.UserCreate;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FlexiCoreApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class UIComponentRESTServiceTest {


    @Autowired
    private TestRestTemplate restTemplate;

    private List<UIComponent> uiComponents;
    private AtomicReference<String> authenticationKey=new AtomicReference<>(null);
    private String otherAuthenticationKey;

    @BeforeAll
    private void init() {
        ResponseEntity<AuthenticationResponse> authenticationResponse = this.restTemplate.postForEntity("/FlexiCore/rest/authenticationNew/login", new AuthenticationRequest().setEmail("admin@flexicore.com").setPassword("admin"), AuthenticationResponse.class);
        authenticationKey.set(authenticationResponse.getBody().getAuthenticationKey());
        restTemplate.getRestTemplate().setInterceptors(
                Collections.singletonList((request, body, execution) -> {
                    request.getHeaders()
                            .add("authenticationKey", authenticationKey.get());
                    return execution.execute(request, body);
                }));

        otherAuthenticationKey = prepareOtherUser();
    }

    private String prepareOtherUser() {

        String name = UUID.randomUUID().toString();
        String email = name + "@test.com";
        String lastName = name + "_Last";
        String password = name + "_pass";
        String phoneNumber = name + "_phone";
        UserCreate request = new UserCreate()
                .setEmail(email)
                .setLastName(lastName)
                .setPassword(password)
                .setPhoneNumber(phoneNumber)
                .setName(name);
        ResponseEntity<User> userResponse = this.restTemplate.postForEntity("/FlexiCore/rest/users/createUser", request, User.class);
        Assertions.assertEquals(200, userResponse.getStatusCodeValue());
        User user = userResponse.getBody();
        ResponseEntity<AuthenticationResponse> authenticationResponse = this.restTemplate.postForEntity("/FlexiCore/rest/authenticationNew/login", new AuthenticationRequest().setEmail(email).setPassword(password), AuthenticationResponse.class);
        return authenticationResponse.getBody().getAuthenticationKey();
    }

    @Test
    @Order(1)
    public void testRegisterNewUIComponent() {

        UIComponentsRegistrationContainer uiComponentsRegistrationContainer = new UIComponentsRegistrationContainer();
        List<UIComponentRegistrationContainer> uiComponentRegistrationContainers = new ArrayList<>();
        Map<String, String> componentToPermissionGroups = new HashMap<>();
        componentToPermissionGroups.put("test1", "a,b,c");
        componentToPermissionGroups.put("test2", "b,c");
        componentToPermissionGroups.put("test3", "c");

        for (String externalId : Arrays.asList("test1", "test2", "test3")) {
            UIComponentRegistrationContainer uiComponentRegistrationContainer = new UIComponentRegistrationContainer();
            uiComponentRegistrationContainer.setExternalId(externalId);
            uiComponentRegistrationContainer.setDescription(externalId);
            uiComponentRegistrationContainer.setName(externalId);
            uiComponentRegistrationContainer.setGroups(componentToPermissionGroups.get(externalId));
        }
        uiComponentsRegistrationContainer.setComponentsToRegister(uiComponentRegistrationContainers);
        ParameterizedTypeReference<List<UIComponent>> t = new ParameterizedTypeReference<List<UIComponent>>() {
        };
        ResponseEntity<List<UIComponent>> uiComponentResponse = this.restTemplate.exchange("/FlexiCore/rest/uiPlugin/registerAndGetAllowedUIComponents", HttpMethod.POST, new HttpEntity<>(uiComponentsRegistrationContainer), t);

        Assertions.assertEquals(200, uiComponentResponse.getStatusCodeValue());
        uiComponents = uiComponentResponse.getBody();
        Assertions.assertNotNull(uiComponents);
        assertUIComponents(uiComponentRegistrationContainers, uiComponents);

    }

    private void assertUIComponents(List<UIComponentRegistrationContainer> uiComponentRegistrationContainers, List<UIComponent> uiComponents) {
        Set<String> expected = uiComponentRegistrationContainers.stream().map(f -> f.getExternalId()).collect(Collectors.toSet());
        Set<String> actual = uiComponents.stream().map(f -> f.getExternalId()).collect(Collectors.toSet());
        Assertions.assertTrue(expected.containsAll(actual));
        Assertions.assertTrue(actual.containsAll(expected));
        Map<String, UIComponent> uiComponentMap = uiComponents.stream().collect(Collectors.toMap(f -> f.getExternalId(), f -> f, (a, b) -> a));
        for (UIComponentRegistrationContainer uiComponentRegistrationContainer : uiComponentRegistrationContainers) {
            UIComponent uiComponent = uiComponentMap.get(uiComponentRegistrationContainer.getExternalId());
            PermissionGroupsFilter permissionGroupsFilter = new PermissionGroupsFilter().setBaseclasses(Collections.singletonList(uiComponent));
            ParameterizedTypeReference<PaginationResponse<PermissionGroup>> t = new ParameterizedTypeReference<PaginationResponse<PermissionGroup>>() {
            };
            ResponseEntity<PaginationResponse<PermissionGroup>> permissionGroupsResponse = this.restTemplate.exchange("/FlexiCore/rest/permissionGroup/getAllPermissionGroups", HttpMethod.POST, new HttpEntity<>(permissionGroupsFilter), t);
            Assertions.assertEquals(200, permissionGroupsResponse.getStatusCodeValue());
            Set<String> actualGroups = permissionGroupsResponse.getBody().getList().stream().map(f -> f.getExternalId()).collect(Collectors.toSet());
            Set<String> expectedGroups = Stream.of(uiComponentRegistrationContainer.getGroups().split(",")).collect(Collectors.toSet());
            Assertions.assertTrue(expectedGroups.containsAll(actualGroups));
            Assertions.assertTrue(actualGroups.containsAll(expectedGroups));
        }

    }


    @Test
    @Order(2)
    public void testRegisterExistingUIComponent() {

        UIComponentsRegistrationContainer uiComponentsRegistrationContainer = new UIComponentsRegistrationContainer();
        List<UIComponentRegistrationContainer> uiComponentRegistrationContainers = new ArrayList<>();
        Map<String, String> componentToPermissionGroups = new HashMap<>();
        componentToPermissionGroups.put("test1", "a,b,c");
        componentToPermissionGroups.put("test2", "b,c");
        componentToPermissionGroups.put("test3", "c");

        for (String externalId : Arrays.asList("test1", "test2", "test3")) {
            UIComponentRegistrationContainer uiComponentRegistrationContainer = new UIComponentRegistrationContainer();
            uiComponentRegistrationContainer.setExternalId(externalId);
            uiComponentRegistrationContainer.setDescription(externalId);
            uiComponentRegistrationContainer.setName(externalId);
            uiComponentRegistrationContainer.setGroups(componentToPermissionGroups.get(externalId));
        }
        uiComponentsRegistrationContainer.setComponentsToRegister(uiComponentRegistrationContainers);
        ParameterizedTypeReference<List<UIComponent>> t = new ParameterizedTypeReference<List<UIComponent>>() {
        };
        ResponseEntity<List<UIComponent>> uiComponentResponse = this.restTemplate.exchange("/FlexiCore/rest/uiPlugin/registerAndGetAllowedUIComponents", HttpMethod.POST, new HttpEntity<>(uiComponentsRegistrationContainer), t);

        Assertions.assertEquals(200, uiComponentResponse.getStatusCodeValue());
        List<UIComponent> uiComponents = uiComponentResponse.getBody();
        Assertions.assertNotNull(uiComponents);
        Map<String, String> expected = this.uiComponents.stream().collect(Collectors.toMap(f -> f.getExternalId(), f -> f.getId()));
        Map<String, String> actual = uiComponents.stream().collect(Collectors.toMap(f -> f.getExternalId(), f -> f.getId()));
        Assertions.assertEquals(actual, expected);


    }


    @Test
    @Order(3)
    public void testRegistrationAsDifferentUser() {
        this.authenticationKey.set(this.otherAuthenticationKey);
        UIComponentsRegistrationContainer uiComponentsRegistrationContainer = new UIComponentsRegistrationContainer();
        List<UIComponentRegistrationContainer> uiComponentRegistrationContainers = new ArrayList<>();
        Map<String, String> componentToPermissionGroups = new HashMap<>();
        componentToPermissionGroups.put("test1", "a,b,c");
        componentToPermissionGroups.put("test2", "b,c");
        componentToPermissionGroups.put("test3", "c");

        for (String externalId : Arrays.asList("test1", "test2", "test3")) {
            UIComponentRegistrationContainer uiComponentRegistrationContainer = new UIComponentRegistrationContainer();
            uiComponentRegistrationContainer.setExternalId(externalId);
            uiComponentRegistrationContainer.setDescription(externalId);
            uiComponentRegistrationContainer.setName(externalId);
            uiComponentRegistrationContainer.setGroups(componentToPermissionGroups.get(externalId));
        }
        uiComponentsRegistrationContainer.setComponentsToRegister(uiComponentRegistrationContainers);
        ParameterizedTypeReference<List<UIComponent>> t = new ParameterizedTypeReference<List<UIComponent>>() {
        };
        ResponseEntity<List<UIComponent>> uiComponentResponse = this.restTemplate.exchange("/FlexiCore/rest/uiPlugin/registerAndGetAllowedUIComponents", HttpMethod.POST, new HttpEntity<>(uiComponentsRegistrationContainer), t);

        Assertions.assertEquals(200, uiComponentResponse.getStatusCodeValue());
        List<UIComponent> uiComponents = uiComponentResponse.getBody();
        Assertions.assertNotNull(uiComponents);
        Assertions.assertTrue(uiComponents.isEmpty());

    }


}
