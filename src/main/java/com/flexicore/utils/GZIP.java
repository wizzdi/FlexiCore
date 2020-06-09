package com.flexicore.utils;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIP {

    public static byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length)) {
            try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
                gzip.write(data);
                return bos.toByteArray();

            }
        }
    }

    public static byte[] decompress(final byte[] compressed) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed)) {
            try (GZIPInputStream gis = new GZIPInputStream(bis)) {
                return IOUtils.toByteArray(gis);

            }
        }

    }
}
