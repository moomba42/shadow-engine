package com.alexdl.sdng.backend.vulkan;

import org.joml.Matrix4f;

public record Model(Mesh mesh, Matrix4f transform) {
}
