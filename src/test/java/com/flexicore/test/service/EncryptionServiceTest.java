package com.flexicore.test.service;

import com.flexicore.init.FlexiCoreApplication;
import com.flexicore.service.impl.EncryptionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
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

    @Test
    @Order(2)
    public void testEncryptOnlyPublicKey() throws GeneralSecurityException, IOException {
        String name = UUID.randomUUID().toString();
        String associated=UUID.randomUUID().toString();
        byte[] encryptingKey = encryptionService.getEncryptingKey();
        com.flexicore.service.EncryptionService.EncryptingKey encryptingKey1 = encryptionService.parseKey(encryptingKey);
        byte[] encryptPublic = encryptingKey1.encrypt(name.getBytes(), associated.getBytes());
        String res = new String(encryptionService.decrypt(encryptPublic, associated.getBytes()));

        Assertions.assertEquals(name,res);


    }


}
