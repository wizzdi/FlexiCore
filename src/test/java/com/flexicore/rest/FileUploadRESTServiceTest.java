package com.flexicore.rest;

import com.amazonaws.util.Md5Utils;
import com.flexicore.data.jsoncontainers.PaginationResponse;
import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.model.FileResource;
import com.flexicore.model.Role;
import com.flexicore.request.AuthenticationRequest;
import com.flexicore.request.RoleCreate;
import com.flexicore.request.RoleFilter;
import com.flexicore.request.RoleUpdate;
import com.flexicore.response.AuthenticationResponse;
import io.joshworks.restclient.http.HttpResponse;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseExtractor;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FlexiCoreApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")

public class FileUploadRESTServiceTest {

    private Role role;
    @Autowired
    private TestRestTemplate restTemplate;
    private static final int FILE_LENGTH=3500000;
    private static final int CHUNK_SIZE = 2000000;

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
    public void testUploadFileFullMd5() {
        Random rd = new Random();
        byte[] data = new byte[FILE_LENGTH];
        rd.nextBytes(data);
        String md5=Hex.encodeHexString(Md5Utils.computeMD5Hash(data));
        String name="test-"+System.currentTimeMillis()+".js";
        String id=null;
        // chunk size to divide
        for(int i=0;i<data.length;i+=CHUNK_SIZE){
            byte[] chunk=Arrays.copyOfRange(data, i, Math.min(data.length,i+CHUNK_SIZE));
            String chunkMD5= Hex.encodeHexString(Md5Utils.computeMD5Hash(chunk));
            boolean lastChunk=i+CHUNK_SIZE >=data.length;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add("md5",md5);
            headers.add("chunkMd5",chunkMD5);
            headers.add("lastChunk",lastChunk+"");
            headers.add("name",name);



            HttpEntity<byte[]> requestEntity = new HttpEntity<>(chunk, headers);

            ResponseEntity<FileResource> response=this.restTemplate.exchange("/FlexiCore/rest/resources/upload", HttpMethod.POST, requestEntity, FileResource.class);
            Assertions.assertEquals(200, response.getStatusCodeValue());
            FileResource fileResource=response.getBody();
            Assertions.assertNotNull(fileResource);
            Assertions.assertEquals(fileResource.isDone(),lastChunk);
            id=fileResource.getId();

        }

        this.restTemplate.execute("/FlexiCore/rest/downloadUnsecure/" + id, HttpMethod.GET, null, (ResponseExtractor<Object>) clientHttpResponse -> {
            File ret = File.createTempFile("download", "tmp");
            StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
            return ret;
        });

    }

    @Test
    @Order(1)
    public void testUploadFileChunkErrorRecovery() {
        Random rd = new Random();
        byte[] data = new byte[FILE_LENGTH];
        rd.nextBytes(data);
        String md5=Hex.encodeHexString(Md5Utils.computeMD5Hash(data));
        String name="test-"+System.currentTimeMillis();
        // chunk size to divide
        boolean error=true;
        for(int i=0;i<data.length;i+=CHUNK_SIZE){
            byte[] chunk=Arrays.copyOfRange(data, i, Math.min(data.length,i+CHUNK_SIZE));
            String chunkMD5= Hex.encodeHexString(Md5Utils.computeMD5Hash(chunk));
            if(error){
                chunkMD5="fake";
            }
            boolean lastChunk=i+CHUNK_SIZE >=data.length;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add("md5",md5);
            headers.add("chunkMd5",chunkMD5);
            headers.add("lastChunk",lastChunk+"");
            headers.add("name",name);



            HttpEntity<byte[]> requestEntity = new HttpEntity<>(chunk, headers);

            ResponseEntity<FileResource> response=this.restTemplate.exchange("/FlexiCore/rest/resources/upload", HttpMethod.POST, requestEntity, FileResource.class);
            if(error){
                Assertions.assertEquals(Response.Status.PRECONDITION_FAILED.getStatusCode(),response.getStatusCodeValue());
                i-=chunk.length;
                error=false;
                continue;
            }
            Assertions.assertEquals(200, response.getStatusCodeValue());
            FileResource fileResource=response.getBody();
            Assertions.assertNotNull(fileResource);
            Assertions.assertEquals(fileResource.isDone(),lastChunk);
        }

    }

    @Test
    @Order(1)
    public void testUploadFileFileErrorRecovery() {
        Random rd = new Random();
        byte[] data = new byte[FILE_LENGTH];
        rd.nextBytes(data);
        byte[] real=data;
        String md5=Hex.encodeHexString(Md5Utils.computeMD5Hash(data));
        String name="test-"+System.currentTimeMillis();
        data= new byte[FILE_LENGTH];
        rd.nextBytes(data);


        // chunk size to divide
        for(int i=0;i<data.length;i+=CHUNK_SIZE){
            byte[] chunk=Arrays.copyOfRange(data, i, Math.min(data.length,i+CHUNK_SIZE));
            String chunkMD5= Hex.encodeHexString(Md5Utils.computeMD5Hash(chunk));

            boolean lastChunk=i+CHUNK_SIZE >=data.length;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add("md5",md5);
            headers.add("chunkMd5",chunkMD5);
            headers.add("lastChunk",lastChunk+"");
            headers.add("name",name);



            HttpEntity<byte[]> requestEntity = new HttpEntity<>(chunk, headers);

            ResponseEntity<FileResource> response=this.restTemplate.exchange("/FlexiCore/rest/resources/upload", HttpMethod.POST, requestEntity, FileResource.class);
            if(lastChunk){
                Assertions.assertEquals(Response.Status.EXPECTATION_FAILED.getStatusCode(), response.getStatusCodeValue());
                break;

            }
            Assertions.assertEquals(200, response.getStatusCodeValue());
            FileResource fileResource=response.getBody();
            Assertions.assertNotNull(fileResource);
            Assertions.assertEquals(fileResource.isDone(),lastChunk);
        }
        data=real;
        // chunk size to divide
        for(int i=0;i<data.length;i+=CHUNK_SIZE){
            byte[] chunk=Arrays.copyOfRange(data, i, Math.min(data.length,i+CHUNK_SIZE));
            String chunkMD5= Hex.encodeHexString(Md5Utils.computeMD5Hash(chunk));

            boolean lastChunk=i+CHUNK_SIZE >=data.length;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add("md5",md5);
            headers.add("chunkMd5",chunkMD5);
            headers.add("lastChunk",lastChunk+"");
            headers.add("name",name);



            HttpEntity<byte[]> requestEntity = new HttpEntity<>(chunk, headers);

            ResponseEntity<FileResource> response=this.restTemplate.exchange("/FlexiCore/rest/resources/upload", HttpMethod.POST, requestEntity, FileResource.class);
            Assertions.assertEquals(200, response.getStatusCodeValue());
            FileResource fileResource=response.getBody();
            Assertions.assertNotNull(fileResource);
            Assertions.assertEquals(fileResource.isDone(),lastChunk);
        }

    }


    @Test
    @Order(2)
    public void testUploadFileNoMd5() {
        Random rd = new Random();
        byte[] data = new byte[FILE_LENGTH];
        rd.nextBytes(data);
        String md5=Hex.encodeHexString(Md5Utils.computeMD5Hash(data));
        String name="test-"+System.currentTimeMillis();
        // chunk size to divide
        for(int i=0;i<data.length;i+=CHUNK_SIZE){
            byte[] chunk=Arrays.copyOfRange(data, i, Math.min(data.length,i+CHUNK_SIZE));
            boolean lastChunk=false;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add("md5",md5);
            headers.add("name",name);



            HttpEntity<byte[]> requestEntity = new HttpEntity<>(chunk, headers);

            ResponseEntity<FileResource> response=this.restTemplate.exchange("/FlexiCore/rest/resources/upload", HttpMethod.POST, requestEntity, FileResource.class);
            Assertions.assertEquals(200, response.getStatusCodeValue());
            FileResource fileResource=response.getBody();
            Assertions.assertNotNull(fileResource);
            Assertions.assertEquals(fileResource.isDone(),lastChunk);
        }

    }


}
