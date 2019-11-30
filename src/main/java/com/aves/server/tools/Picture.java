package com.aves.server.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public class Picture {
    static private final SecureRandom random = new SecureRandom();

    private byte[] imageData;
    private String mimeType;
    private int width;
    private int height;
    private int size;
    private byte[] otrKey;
    private byte[] encBytes = null;
    private byte[] sha256;
    private String assetKey;
    private String assetToken;
    private boolean isPublic;
    private String retention = "eternal";
    private UUID messageId = UUID.randomUUID();
    private long expires;

    public Picture(byte[] bytes, String mime) throws IOException {
        imageData = bytes;
        size = bytes.length;
        mimeType = mime;
        BufferedImage bufferedImage = loadBufferImage(bytes);
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
    }

    public Picture(byte[] bytes) throws IOException {
        imageData = bytes;
        mimeType = Util.extractMimeType(imageData);
        size = bytes.length;
        BufferedImage bufferedImage = loadBufferImage(bytes);
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
    }

    public Picture(String url) throws IOException {
        try (InputStream input = new URL(url).openStream()) {
            imageData = Util.toByteArray(input);
        }
        mimeType = Util.extractMimeType(imageData);
        size = imageData.length;
        BufferedImage bufferedImage = loadBufferImage(imageData);
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
    }

    public Picture() {
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getRetention() {
        return retention;
    }

    public void setRetention(String retention) {
        this.retention = retention;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public byte[] getOtrKey() {
        if (otrKey == null) {
            otrKey = new byte[32];
            random.nextBytes(otrKey);
        }
        return otrKey;
    }

    public void setOtrKey(byte[] otrKey) {
        this.otrKey = otrKey;
    }

    public byte[] getSha256() throws NoSuchAlgorithmException {
        if (sha256 == null) {
            sha256 = MessageDigest.getInstance("SHA-256").digest(encBytes);
        }
        return sha256;
    }

    public void setSha256(byte[] sha256) {
        this.sha256 = sha256;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public void setAssetKey(String assetKey) {
        this.assetKey = assetKey;
    }

    public String getAssetToken() {
        return assetToken;
    }

    public void setAssetToken(String assetToken) {
        this.assetToken = assetToken;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    private BufferedImage loadBufferImage(byte[] imageData) throws IOException {
        try (InputStream input = new ByteArrayInputStream(imageData)) {
            return ImageIO.read(input);
        }
    }
}
