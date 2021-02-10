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
                AeadConfig.register();
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
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            KeysetHandle publicKeysetHandle = keysetHandle.getPublicKeysetHandle();
            CleartextKeysetHandle.write(publicKeysetHandle, JsonKeysetWriter.withOutputStream(outputStream));
            return outputStream.toByteArray();

        } catch (Exception e) {
            logger.debug("failed getting encrypting key , attempting old format", e);
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(outputStream));
                return outputStream.toByteArray();
            }
            catch (Exception e1){
                logger.error("failed getting encrypting key , attempting old format", e1);

            }
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
        return encryptWithFallback(keysetHandle,plaintext, associatedData);

        // 3. Use the primitive.

    }

    private static byte[] encryptWithFallback(KeysetHandle keysetHandle,byte[] plaintext, byte[] associatedData) throws GeneralSecurityException {
        try {
            HybridEncrypt hybridEncrypt =
                    keysetHandle.getPublicKeysetHandle().getPrimitive(HybridEncrypt.class);
            return hybridEncrypt.encrypt(plaintext, associatedData);

        }
        catch (GeneralSecurityException e){
            logger.debug("failed encrypting , retrying with old key format",e);
            Aead primitive =
                    keysetHandle.getPrimitive(Aead.class);
            return primitive.encrypt(plaintext, associatedData);
        }
    }

    @Override
    public byte[] decrypt(final byte[] ciphertext, final byte[] associatedData) throws GeneralSecurityException{
        initEncryption();
        return decryptWithFallback(keysetHandle,ciphertext, associatedData);

    }

    private static byte[] decryptWithFallback(KeysetHandle keysetHandle,byte[] ciphertext, byte[] associatedData) throws GeneralSecurityException {
        try {
            HybridDecrypt hybridDecrypt =
                    keysetHandle.getPrimitive(HybridDecrypt.class);
            return hybridDecrypt.decrypt(ciphertext, associatedData);

        }
        catch (GeneralSecurityException e){
            logger.debug("failed Decrypting , retrying with old key format",e);
            Aead aead =
                    keysetHandle.getPrimitive(Aead.class);
            return aead.decrypt(ciphertext, associatedData);
        }
    }

    static class HybridEncryptImpl implements EncryptingKey{
        private KeysetHandle keysetHandle;

        public HybridEncryptImpl(KeysetHandle aead) {
            this.keysetHandle = aead;
        }

        @Override
        public byte[] encrypt(byte[] plaintext, byte[] associatedData) throws GeneralSecurityException {
            return encryptWithFallback(keysetHandle,plaintext,associatedData);
        }

        @Override
        public byte[] decrypt(byte[] ciphertext, byte[] associatedData) throws GeneralSecurityException {
            throw new UnsupportedOperationException("does not support decrypting");
        }
    }

}
