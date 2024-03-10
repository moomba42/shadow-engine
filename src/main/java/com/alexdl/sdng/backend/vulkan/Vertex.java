package com.alexdl.sdng.backend.vulkan;

import org.joml.Vector3f;

public record Vertex(
        Vector3f position,
        Vector3f color
) {
    public static final int POSITION_OFFSET_BYTES = 0;
    public static final int POSITION_SIZE_BYTES = Float.BYTES * 3;
    public static final int COLOR_OFFSET_BYTES = POSITION_SIZE_BYTES;
    public static final int COLOR_SIZE_BYTES = Float.BYTES * 3;
    public static final int BYTES = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES;

    public Vertex(float x, float y, float z, float r, float g, float b) {
        this(new Vector3f(x, y, z), new Vector3f(r, g, b));
    }
}
