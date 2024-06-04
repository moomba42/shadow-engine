package com.alexdl.sdng.backend.vulkan.structs;

public class MemoryAlignment {
    private final int size;
    private final int alignment;
    private int offset;
    private int alignedSize;

    public MemoryAlignment(int alignment, int size) {
        this.alignment = alignment;
        this.size = size;
    }

    private void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public int getAlignedSize() {
        return alignedSize;
    }

    public void setAlignedSize(int alignedSize) {
        this.alignedSize = alignedSize;
    }

    public int getSize() {
        return size;
    }

    public static MemoryAlignment align(int alignment, MemoryAlignment... alignments) {
        int offset = 0;
        for (MemoryAlignment element : alignments) {
            int alignedSize = align(element.alignment, element.size);
            int alignedOffset = align(element.alignment, offset);
            element.setOffset(alignedOffset);
            element.setAlignedSize(alignedSize);
            offset = alignedOffset + alignedSize;
        }
        return new MemoryAlignment(alignment, align(alignment, offset)); // aligned size
    }

    private static int align(int alignment, int bytes) {
        return (bytes + alignment - 1) & -alignment;
    }

    public static MemoryAlignment glslInt() {
        return new MemoryAlignment(4, 4);
    }

    public static MemoryAlignment glslFloat() {
        return new MemoryAlignment(4, 4);
    }

    public static MemoryAlignment glslVec2() {
        return new MemoryAlignment(8, 8);
    }

    public static MemoryAlignment glslVec3() {
        return new MemoryAlignment(16, 12);
    }

    public static MemoryAlignment glslVec4() {
        return new MemoryAlignment(16, 16);
    }
}
