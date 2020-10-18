package com.flexicore.service;

import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.service.impl.EncryptionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.GeneralSecurityException;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FlexiCoreApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class EncryptionServiceTest {

    @Autowired
    private EncryptionService encryptionService;




    @Test
    @Order(1)
    public void testEncrypt() throws GeneralSecurityException {
        String name = UUID.randomUUID().toString();
        String associated=UUID.randomUUID().toString();
        byte[] encrypt = encryptionService.encrypt(name.getBytes(), associated.getBytes());
        String res = new String(encryptionService.decrypt(encrypt, associated.getBytes()));
        Assertions.assertEquals(name,res);

    }


}
