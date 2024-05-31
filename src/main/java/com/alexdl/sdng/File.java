package com.alexdl.sdng;

import java.nio.ByteBuffer;

public record File(FileHandle handle, ByteBuffer dataBuffer) {
}
