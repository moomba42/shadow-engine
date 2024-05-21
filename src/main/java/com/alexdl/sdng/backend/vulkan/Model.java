package com.alexdl.sdng.backend.vulkan;

import org.joml.Matrix4f;

public record Model(Mesh mesh, Texture texture, Matrix4f transform) {
}
