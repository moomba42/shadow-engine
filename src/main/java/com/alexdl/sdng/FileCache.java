package com.alexdl.sdng;

public interface FileCache {
    File put(File file);
    boolean containsKey(FileHandle handle);
    File get(FileHandle handle);
    void clear();
}
