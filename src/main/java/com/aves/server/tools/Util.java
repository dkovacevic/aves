package com.aves.server.tools;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;

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
import java.util.Random;
import java.util.UUID;

public class Util {
    private static final SecureRandom random = new SecureRandom();
    private static final String BUCKET_NAME = "aves-bucket";

    private static MinioClient minioClient;
    private static SendGrid sendgrid;

    static {
        try {
            minioClient = new MinioClient("http://play.min.io",
                    "Q3AM3UQ867SPQQA43P2F",
                    "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");
        } catch (InvalidEndpointException | InvalidPortException e) {
            e.printStackTrace();
        }
        try {
            String key = System.getenv("SENDGRID_API_KEY");
            sendgrid = new SendGrid(key);
            sendgrid.addRequestHeader("X-Mock", "true");
        } catch (Exception e) {
            Logger.error("SendGrid: %s", e);
        }
    }

    public static UUID s3UploadFile(byte[] bytes) throws Exception {
        if (!minioClient.bucketExists(BUCKET_NAME))
            minioClient.makeBucket(BUCKET_NAME);

        UUID key = UUID.randomUUID();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    BUCKET_NAME,
                    key.toString(),
                    bais,
                    (long) bytes.length,
                    null,
                    null,
                    "application/octet-stream");
        }
        return key;
    }

    public static InputStream s3DownloadFIle(UUID assetId) throws Exception {
        return minioClient.getObject(BUCKET_NAME, assetId.toString());
    }

    private static String next() {
        return new BigInteger(130, random).toString(32);
    }

    public static String next(int length) {
        return next().substring(0, length);
    }

    public static int random(int bound) {
        return random.nextInt(bound);
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

    static String extractMimeType(byte[] imageData) throws IOException {
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

    public static Picture getProfilePicture() throws Exception {
        String filename = String.format("profiles/%d.png", new Random().nextInt(8));
        InputStream is = Util.class.getClassLoader().getResourceAsStream(filename);
        byte[] image = toByteArray(is);
        return ImageProcessor.getMediumImage(new Picture(image));
    }

    public static Mail createMail(String subject, String body, String from, String to) {
        return new Mail(new Email(from), subject, new Email(to), new Content("text/HTML", body));
    }

    public static boolean sendEmail(String subject, String body, String from, String to) {
        try {
            Mail mail = createMail(subject, body, from, to);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendgrid.api(request);
            return response.getStatusCode() < 300;
        } catch (Exception e) {
            Logger.error("sendEmail: %s", e);
            return false;
        }
    }
}
