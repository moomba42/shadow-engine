package com.alexdl.sdng.backend.vulkan.structs;

//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//import java.nio.ByteBuffer;
//
//import static org.lwjgl.system.Checks.CHECKS;
//import static org.lwjgl.system.MemoryUtil.*;

public abstract class Struct<Field extends Enum<Field>> {
//    private static final int DEFAULT_ALIGNMENT = 0;
//
//    private final long address;
//    private @Nullable ByteBuffer container;
//    private final int sizeBytes;
//    private final int alignmentBytes;
//    private final int alignedSizeBytes;
//
//    public Struct(long address, int alignmentBytes) {
//        if(CHECKS && address == NULL) {
//            throw new NullPointerException();
//        }
//        this.address = address;
//        this.alignmentBytes = alignmentBytes;
//        this.sizeBytes = getSizeBytes();
//        this.alignedSizeBytes = (this.sizeBytes + this.alignmentBytes - 1) & (~(this.alignmentBytes - 1));
//    }
//
//    public Struct(long address) {
//        this(address, DEFAULT_ALIGNMENT);
//    }
//
//    public Struct(@Nonnull ByteBuffer container) {
//        this(memAddress(container));
//        this.container = container;
//    }
//
//    public Struct(@Nonnull ByteBuffer container, int alignmentBytes) {
//        this(memAddress(container), alignmentBytes);
//        this.container = container;
//    }
//
//    abstract int getSizeBytes();
//
//    public void free() {
//        nmemFree(address);
//    }
//
//    public void clear() {
//        memSet(address, 0, sizeBytes);
//    }
}
