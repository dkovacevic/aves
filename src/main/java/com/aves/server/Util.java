package com.aves.server;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Util {
    private static final SecureRandom random = new SecureRandom();

    private static String next() {
        return new BigInteger(130, random).toString(32);
    }

    public static String next(int length) {
        return next().substring(0, length);
    }
}
