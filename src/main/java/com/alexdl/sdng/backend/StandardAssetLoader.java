package com.alexdl.sdng.backend;

import com.alexdl.sdng.AssetLoader;
import com.alexdl.sdng.Disposables;
import com.alexdl.sdng.backend.vulkan.Material;
import com.alexdl.sdng.backend.vulkan.Mesh;
import com.alexdl.sdng.backend.vulkan.MeshData;
import com.alexdl.sdng.backend.vulkan.Model;
import com.alexdl.sdng.backend.vulkan.ResourceHandle;
import com.alexdl.sdng.backend.vulkan.Texture;
import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import com.alexdl.sdng.backend.vulkan.structs.VertexDataStruct;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class StandardAssetLoader implements AssetLoader {
    private final VulkanRenderer renderer;
    private final Disposables disposables;

    @Inject
    public StandardAssetLoader(VulkanRenderer renderer, Disposables disposables) {
        this.renderer = renderer;
        this.disposables = disposables;
    }

    @Nonnull
    public Model loadModel(@Nonnull ResourceHandle resourceHandle) {
        ByteBuffer fileData = loadResource(resourceHandle);

        AIScene aiScene = aiImportFileFromMemory(fileData, aiProcess_Triangulate | aiProcess_FlipUVs | aiProcess_JoinIdenticalVertices, (ByteBuffer) null);

        if (aiScene == null) {
            throw new RuntimeException("Error loading model");
        }
        System.out.println("-----");
        System.out.println("Parsing resource: " + resourceHandle.uri());

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        assert aiMaterials != null;
        List<Material> materials = new ArrayList<>(numMaterials);
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            Material material = parseMaterial(aiMaterial);
            materials.add(material);
        }

        System.out.println("Results:");
        System.out.println("\tNum materials: " + materials.size());

        aiScene.free();

        VertexDataStruct.Buffer quad = new VertexDataStruct.Buffer(new float[]{
                -0.25f, 0.6f, 0, 1, 0, 0, 1, 1,
                -0.25f, -0.6f, 0, 0, 1, 0, 1, 0,
                0.25f, -0.6f, 0, 0, 0, 1, 0, 0,
                0.25f, 0.6f, 0, 1, 1, 0, 0, 1,
        });
        IntBuffer quadIndices = BufferUtils.createIntBuffer(6).put(0, new int[]{0, 1, 2, 2, 3, 0});

        MeshData data = new MeshData(
                renderer.getGraphicsQueue(),
                renderer.getGraphicsCommandPool(),
                quad, quadIndices
        );

        quad.dispose();
        disposables.add(data);

        Texture diffuse = loadTexture(new ResourceHandle("art.png"));
        Material material = new Material(diffuse, new Vector4f(1, 1, 1, 1));

        return new Model(new Mesh(data, material), new Matrix4f().identity());
    }

    private Material parseMaterial(AIMaterial aiMaterial) {
        AIColor4D color = AIColor4D.create();
        AIString path = AIString.calloc();

        Assimp.aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null, null, null, null, null, null);
        String diffuseTexturePath = path.dataString();
        Texture diffuseTexture = null;
        if (!diffuseTexturePath.isEmpty()) {
            System.out.println("Loading material texture path: " + diffuseTexturePath);
            // Load diffuse texture here
        }

        Vector4f diffuseColor = new Vector4f();
        int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color);
        if (result == 0) {
            diffuseColor = new Vector4f(color.r(), color.g(), color.b(), color.a());
        }

        color.free();
        path.free();

        return new Material(diffuseTexture, diffuseColor);
    }

    @NotNull
    @Override
    public Texture loadTexture(@NotNull ResourceHandle resourceHandle) {
        return renderer.createTexture(resourceHandle.uri());
    }

    private ByteBuffer loadResource(@NotNull ResourceHandle resourceHandle) {
        InputStream file = StandardAssetLoader.class.getClassLoader().getResourceAsStream(resourceHandle.uri());
        if (file == null) {
            throw new RuntimeException("Could not open file as resource: " + resourceHandle.uri());
        }
        try {
            ByteBuffer rawDataBuffer = BufferUtils.createByteBuffer(file.available()).put(file.readAllBytes()).flip();
            file.close();
            return rawDataBuffer;
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource into a buffer", e);
        }
    }
}
