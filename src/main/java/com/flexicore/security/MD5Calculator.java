/*******************************************************************************
 *  Copyright (C) FlexiCore, Inc - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Avishay Ben Natan And Asaf Ben Natan, October 2015
 ******************************************************************************/
package com.flexicore.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Calculator {
    public static String getMD5(String input) {

        byte[] inputBytes = input.getBytes();
        return getMD5(inputBytes);
    }

    public static String getMD5(byte[] inputBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            md.update(inputBytes);
            //Get the hash's bytes
            byte[] bytes = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            //Get complete hashed password in hex format
            return sb.toString();
        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }
    }
}
