package com.flexicore.init;

import com.lambdaworks.crypto.SCryptUtil;

import java.io.IOException;

public class Main {


    private static final int scryptN=16384;
    private static final int scryptR=8;
    private static final int scryptP=1;
    public static void main(String[] args) throws IOException {
        System.out.println(setdecryptedPassword("admin"));
    }


    public static String setdecryptedPassword(String password) {
       return SCryptUtil.scrypt(password, scryptN, scryptR, scryptP);
    }
}
