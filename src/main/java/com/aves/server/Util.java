package com.aves.server;

import javax.annotation.Nullable;
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

    @Nullable
    public static String getQueryParam(String query, String queryParam) {
        for (String pair : query.split("&")) {
            String[] split = pair.split("=");
            String name = split[0];
            String value = split[1];
            if (name.equalsIgnoreCase(queryParam))
                return value;
        }

        return null;
    }
}
