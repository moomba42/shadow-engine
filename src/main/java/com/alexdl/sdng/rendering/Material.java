package com.alexdl.sdng.rendering;

import org.joml.Vector3f;

/**
 * Defines how to render a mesh.
 *
 * @param diffuseTexture A diffuse texture applied to the mesh
 * @param diffuseColor A tint applied to the mesh
 * @param specularColor The color of the reflections (multiplied with light color) and its opacity (magnitude)
 * @param specularExponent The sharpness of reflections (higher is sharper, ranged 0 to 1000)
 */
public record Material(
        Texture diffuseTexture,
        Vector3f diffuseColor,
        Vector3f specularColor,
        float specularExponent) {
}
