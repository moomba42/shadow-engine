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
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFile;
import org.lwjgl.assimp.AIFileIO;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.*;

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
        AIScene aiScene = loadAssimpScene(resourceHandle);

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        List<Material> materials = new ArrayList<>(numMaterials);
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            Material material = parseMaterial(aiMaterial);
            materials.add(material);
        }

        aiReleaseImport(aiScene);

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

    @Nonnull
    private Material parseMaterial(@Nonnull AIMaterial aiMaterial) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIColor4D color = AIColor4D.calloc(stack);
            AIString path = AIString.calloc(stack);

            aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, path, (IntBuffer) null,
                    null, null, null, null, null);
            String diffuseTexturePath = path.dataString();
            Texture diffuseTexture = null;
            if (!diffuseTexturePath.isEmpty()) {
                // Load diffuse texture here
                diffuseTexture = loadTexture(new ResourceHandle(diffuseTexturePath));
            }

            Vector4f diffuseColor = new Vector4f();
            int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color);
            if (result == 0) {
                diffuseColor = new Vector4f(color.r(), color.g(), color.b(), color.a());
            }
            return new Material(diffuseTexture, diffuseColor);
        }
    }

    @Override
    @Nonnull
    public Texture loadTexture(@Nonnull ResourceHandle resourceHandle) {
        return renderer.createTexture(resourceHandle.uri());
    }

    @Nonnull
    private ByteBuffer loadResource(@Nonnull ResourceHandle resourceHandle) {
        InputStream file = StandardAssetLoader.class.getClassLoader().getResourceAsStream(resourceHandle.uri());
        if (file == null) {
            throw new RuntimeException("Could not open file as resource: " + resourceHandle.uri());
        }
        try {
            ByteBuffer rawDataBuffer = BufferUtils.createByteBuffer(file.available()).put(file.readAllBytes()).flip();
            file.close();
            return rawDataBuffer;
        } catch (IOException e) {
            throw new RuntimeException("Could not read as a resource into a buffer", e);
        }
    }

    @Nonnull
    private AIScene loadAssimpScene(@Nonnull ResourceHandle resourceHandle) {
        AIFileIO fileIO = AIFileIO.create()
                .OpenProc((pFileIo, fileName, openMode) -> {
                    ByteBuffer data = loadResource(new ResourceHandle(memUTF8(fileName)));

                    return AIFile.create()
                            .ReadProc((pFile, pBuffer, size, count) -> {
                                long max = Math.min(data.remaining() / size, count);
                                memCopy(memAddress(data), pBuffer, max * size);
                                data.position(data.position() + (int) (max * size));
                                return max;
                            })
                            .SeekProc((pFile, offset, origin) -> {
                                if (origin == aiOrigin_CUR) {
                                    data.position(data.position() + (int) offset);
                                } else if (origin == aiOrigin_SET) {
                                    data.position((int) offset);
                                } else if (origin == aiOrigin_END) {
                                    data.position(data.limit() + (int) offset);
                                }
                                return 0;
                            })
                            .FileSizeProc(pFile -> data.limit())
                            .address();
                })
                .CloseProc((pFileIO, pFile) -> {
                    AIFile aiFile = AIFile.create(pFile);

                    aiFile.ReadProc().free();
                    aiFile.SeekProc().free();
                    aiFile.FileSizeProc().free();
                });
        AIScene aiScene = aiImportFileEx(resourceHandle.uri(),
                aiProcess_Triangulate | aiProcess_FlipUVs | aiProcess_JoinIdenticalVertices, fileIO);
        fileIO.OpenProc().free();
        fileIO.CloseProc().free();

        if (aiScene == null) {
            throw new RuntimeException("Error loading resource as an assimp scene: " + resourceHandle.uri());
        }

        return aiScene;
    }
}
