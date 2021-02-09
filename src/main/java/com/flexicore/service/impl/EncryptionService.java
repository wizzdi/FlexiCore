package com.flexicore.service.impl;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKeyManager;
import com.google.crypto.tink.hybrid.EciesAeadHkdfPrivateKeyManager;
import com.google.crypto.tink.hybrid.HybridConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicBoolean;


@Primary
@Component
public class EncryptionService implements com.flexicore.service.EncryptionService {
    private static  KeysetHandle keysetHandle;


    private  static final Logger logger=LoggerFactory.getLogger(EncryptionService.class);

    @Value("${flexicore.security.encryption.tinkKeySetPath:/home/flexicore/keyset.json}")
    private String tinkKeySetPath;



    private static AtomicBoolean init=new AtomicBoolean(false);


    private void initEncryption() {
        if(init.compareAndSet(false,true)){
            try {
                HybridConfig.register();
                File keysetFile = new File(tinkKeySetPath);
                if (!keysetFile.exists()) {
                    // 1. Generate the private key material.
                    keysetHandle = KeysetHandle.generateNew(EciesAeadHkdfPrivateKeyManager.eciesP256HkdfHmacSha256Aes128GcmTemplate());

                    CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withFile(keysetFile));
                } else {
                    keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(keysetFile));
                }


            } catch (Exception e) {
                logger.error( "failed loading keyHandle", e);
            }

        }


    }
    @Override
    public byte[] getEncryptingKey() {
        initEncryption();
        try {
            File keysetFile = new File(tinkKeySetPath);
            KeysetHandle keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(keysetFile));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CleartextKeysetHandle.write(keysetHandle.getPublicKeysetHandle(), JsonKeysetWriter.withOutputStream(outputStream));
            return outputStream.toByteArray();

        } catch (Exception e) {
            logger.error("failed getting encrypting key", e);
        }
        return null;


    }


    @Override
    public EncryptingKey parseKey(byte[] encryptingKey) throws IOException,GeneralSecurityException {
        initEncryption();
        KeysetHandle keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withBytes(encryptingKey));
        return new HybridEncryptImpl(keysetHandle);
    }

    @Override
    public byte[] encrypt(final byte[] plaintext, final byte[] associatedData) throws GeneralSecurityException{
        initEncryption();
        HybridEncrypt hybridEncrypt =
                keysetHandle.getPublicKeysetHandle().getPrimitive(HybridEncrypt.class);

        // 3. Use the primitive.
        return hybridEncrypt.encrypt(plaintext, associatedData);

    }

    @Override
    public byte[] decrypt(final byte[] ciphertext, final byte[] associatedData) throws GeneralSecurityException{
        initEncryption();
        HybridDecrypt hybridDecrypt =
                keysetHandle.getPrimitive(HybridDecrypt.class);
        return hybridDecrypt.decrypt(ciphertext,associatedData);
    }

    static class HybridEncryptImpl implements EncryptingKey{
        private KeysetHandle keysetHandle;

        public HybridEncryptImpl(KeysetHandle aead) {
            this.keysetHandle = aead;
        }

        @Override
        public byte[] encrypt(byte[] plaintext, byte[] associatedData) throws GeneralSecurityException {
            HybridEncrypt hybridEncrypt =
                    keysetHandle.getPrimitive(HybridEncrypt.class);

            // 3. Use the primitive.
            return hybridEncrypt.encrypt(plaintext, associatedData);
        }

        @Override
        public byte[] decrypt(byte[] ciphertext, byte[] associatedData) throws GeneralSecurityException {
            throw new UnsupportedOperationException("does not support decrypting");
        }
    }

}
