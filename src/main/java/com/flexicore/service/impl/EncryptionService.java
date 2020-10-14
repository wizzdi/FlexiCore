package com.flexicore.service.impl;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.proto.KeyTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;


@Primary
@Component
public class EncryptionService implements com.flexicore.service.EncryptionService {
    private static Aead aead;

    @Autowired
    private  Logger logger;

    @Value("${flexicore.security.encryption.tinkKeySetPath:/home/flexicore/keyset.json}")
    private String tinkKeySetPath;



    private static AtomicBoolean init=new AtomicBoolean(false);


    private void initEncryption() {
        if(init.compareAndSet(false,true)){
            try {
                TinkConfig.register();
                File keysetFile = new File(tinkKeySetPath);
                KeysetHandle keysetHandle;
                if (!keysetFile.exists()) {
                    KeyTemplate keyTemplate = AeadKeyTemplates.AES128_GCM;
                    keysetHandle = KeysetHandle.generateNew(keyTemplate);
                    CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(keysetFile));
                } else {
                    keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(keysetFile));
                }
                aead = AeadFactory.getPrimitive(keysetHandle);


            } catch (Exception e) {
                logger.log(Level.SEVERE, "failed loading keyHandle", e);
            }

        }


    }

    @Override
    public byte[] encrypt(final byte[] plaintext, final byte[] associatedData) throws GeneralSecurityException{
        initEncryption();
        return aead.encrypt(plaintext,associatedData);

    }

    @Override
    public byte[] decrypt(final byte[] ciphertext, final byte[] associatedData) throws GeneralSecurityException{
        initEncryption();
        return aead.decrypt(ciphertext,associatedData);
    }

}
