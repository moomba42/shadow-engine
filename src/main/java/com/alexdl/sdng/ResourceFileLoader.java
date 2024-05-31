package com.alexdl.sdng;

import com.alexdl.sdng.backend.ResourceAssetLoader;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ResourceFileLoader implements FileLoader {
    private final FileCache cache;

    @Inject
    public ResourceFileLoader(FileCache cache) {
        this.cache = cache;
    }

    @Nonnull
    @Override
    public File loadFile(@Nonnull FileHandle resourceHandle) {
        if (cache.containsKey(resourceHandle)) {
            return cache.get(resourceHandle);
        }

        InputStream fileStream = ResourceAssetLoader.class.getClassLoader().getResourceAsStream(resourceHandle.uri());
        if (fileStream == null) {
            throw new RuntimeException("Could not open file as resource: " + resourceHandle.uri());
        }

        try {
            ByteBuffer rawDataBuffer = BufferUtils.createByteBuffer(fileStream.available()).put(fileStream.readAllBytes()).flip();
            fileStream.close();
            File file = new File(resourceHandle, rawDataBuffer.asReadOnlyBuffer());
            cache.put(file);
            return file;
        } catch (IOException e) {
            throw new RuntimeException("Could not read as a resource into a buffer", e);
        }
    }
}
