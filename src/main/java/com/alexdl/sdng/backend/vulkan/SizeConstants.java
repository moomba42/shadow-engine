package com.alexdl.sdng.backend.vulkan;

public class SizeConstants {
    public static final int MATRIX_4F_SIZE_FLOATS = 4 * 4;
    public static final int MATRIX_4F_SIZE_BYTES = MATRIX_4F_SIZE_FLOATS * Float.BYTES;

    public static final int VECTOR_4F_SIZE_FLOATS = 4;
    public static final int VECTOR_4F_SIZE_BYTES = VECTOR_4F_SIZE_FLOATS * Float.BYTES;

    public static final int VECTOR_3F_SIZE_FLOATS = 3;
    public static final int VECTOR_3F_SIZE_BYTES = VECTOR_3F_SIZE_FLOATS * Float.BYTES;

    public static final int VECTOR_2F_SIZE_FLOATS = 2;
    public static final int VECTOR_2F_SIZE_BYTES = VECTOR_2F_SIZE_FLOATS * Float.BYTES;
}
