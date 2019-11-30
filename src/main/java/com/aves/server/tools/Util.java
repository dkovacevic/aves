package com.aves.server.tools;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

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

    public static byte[] toByteArray(InputStream input) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int n;
            byte[] buffer = new byte[1024 * 4];
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        }
    }

    public static String extractMimeType(byte[] imageData) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(imageData)) {
            String contentType = URLConnection.guessContentTypeFromStream(input);
            return contentType != null ? contentType : "image/xyz";
        }
    }

    public static String calcMd5(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(bytes, 0, bytes.length);
        byte[] hash = md.digest();
        byte[] byteArray = Base64.getEncoder().encode(hash);
        return new String(byteArray);
    }
}
