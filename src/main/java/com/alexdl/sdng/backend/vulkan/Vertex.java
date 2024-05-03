package com.alexdl.sdng.backend.vulkan;

import org.joml.Vector2f;
import org.joml.Vector3f;

public record Vertex(
        Vector3f position,
        Vector3f color,
        Vector2f uv
) {
    public static final int POSITION_OFFSET_BYTES = 0;
    public static final int POSITION_SIZE_BYTES = Float.BYTES * 3;
    public static final int COLOR_OFFSET_BYTES = POSITION_SIZE_BYTES;
    public static final int COLOR_SIZE_BYTES = Float.BYTES * 3;
    public static final int UV_OFFSET_BYTES = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES;
    public static final int UV_SIZE_BYTES = Float.BYTES * 2;
    public static final int BYTES = POSITION_SIZE_BYTES + COLOR_SIZE_BYTES + UV_SIZE_BYTES;

    public Vertex(float x, float y, float z, float r, float g, float b, float u, float v) {
        this(new Vector3f(x, y, z), new Vector3f(r, g, b), new Vector2f(u, v));
    }
}
