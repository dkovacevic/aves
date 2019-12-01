package com.aves.server.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Picture {
    private byte[] imageData;
    private String mimeType;
    private int width;
    private int height;
    private boolean isPublic;
    private String retention = "eternal";
    private int size;

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

    public String getMimeType() {
        return mimeType;
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

    public int getHeight() {
        return height;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public int getSize() {
        return size;
    }

    private BufferedImage loadBufferImage(byte[] imageData) throws IOException {
        try (InputStream input = new ByteArrayInputStream(imageData)) {
            return ImageIO.read(input);
        }
    }
}
