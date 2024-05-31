package com.alexdl.sdng.backend.vulkan;

import org.joml.Matrix4f;

import java.util.List;

public record Model(List<Mesh> meshes, Matrix4f transform) {
}
