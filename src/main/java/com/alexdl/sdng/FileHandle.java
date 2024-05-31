package com.alexdl.sdng;

public record FileHandle(String uri) {
    @Override
    public String toString() {
        return "ResourceHandle{" +
               "uri='" + uri + '\'' +
               '}';
    }
}
