package com.example.battle_assistant.preparation;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

public final class ImageInput {
    private ImageInput() {}
    public static BufferedImage decode(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > 8_000_000) throw new IllegalArgumentException("图片大小应在 1 字节至 8MB 之间");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("仅支持 PNG/JPEG 图片");
            var reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg"))
                    throw new IllegalArgumentException("仅支持 PNG/JPEG 图片");
                reader.setInput(input);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 16 || h < 16 || (long)w*h > 16_000_000)
                    throw new IllegalArgumentException("图片过小或超过 1600 万像素");
                return reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException e) { throw new IllegalArgumentException("图片损坏或无法解码"); }
    }
}
