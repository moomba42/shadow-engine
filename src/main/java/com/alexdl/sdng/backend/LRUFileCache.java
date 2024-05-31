package com.alexdl.sdng.backend;

import com.alexdl.sdng.File;
import com.alexdl.sdng.FileCache;
import com.alexdl.sdng.FileHandle;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUFileCache extends LinkedHashMap<FileHandle, File> implements FileCache {

    private final int maxSize;

    public LRUFileCache(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<FileHandle, File> eldest) {
        return super.size() > maxSize;
    }

    @Override
    public File put(File file) {
        return super.put(file.handle(), file);
    }

    @Override
    public boolean containsKey(FileHandle handle) {
        return super.containsKey(handle);
    }

    @Override
    public File get(FileHandle handle) {
        return super.get(handle);
    }
}
