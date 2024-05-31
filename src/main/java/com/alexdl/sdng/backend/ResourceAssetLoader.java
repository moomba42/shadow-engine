package com.alexdl.sdng.backend;

import com.alexdl.sdng.AssetLoader;
import com.alexdl.sdng.Disposables;
import com.alexdl.sdng.FileHandle;
import com.alexdl.sdng.ResourceFileLoader;
import com.alexdl.sdng.backend.vulkan.Material;
import com.alexdl.sdng.backend.vulkan.Mesh;
import com.alexdl.sdng.backend.vulkan.MeshData;
import com.alexdl.sdng.backend.vulkan.Model;
import com.alexdl.sdng.backend.vulkan.Texture;
import com.alexdl.sdng.backend.vulkan.VulkanRenderer;
import com.alexdl.sdng.backend.vulkan.structs.VertexDataStruct;
import com.alexdl.sdng.logging.Logger;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIFile;
import org.lwjgl.assimp.AIFileIO;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ResourceAssetLoader implements AssetLoader {
    private final VulkanRenderer renderer;
    private final ResourceFileLoader fileLoader;
    private final Disposables disposables;
    private final Logger logger;

    @Inject
    public ResourceAssetLoader(VulkanRenderer renderer, ResourceFileLoader fileLoader, Disposables disposables, Logger logger) {
        this.renderer = renderer;
        this.fileLoader = fileLoader;
        this.disposables = disposables;
        this.logger = logger;
    }

    @Nonnull
    public Model loadModel(@Nonnull FileHandle resourceHandle) {
        logger.info("-----------------");
        logger.info("Loading model: %s", resourceHandle);
        AIScene aiScene = loadAssimpScene(resourceHandle);

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        List<Material> materials = new ArrayList<>(numMaterials);
        for (int i = 0; i < numMaterials; i++) {
            logger.info("Parsing material %d", i);
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            Material material = parseMaterial(aiMaterial);
            materials.add(material);
        }

        if(numMaterials != materials.size()) {
            logger.warn("Parsed materials size is not the same as loaded materials size! loaded=%d, parsed=%d, uri=%s",
                    numMaterials, materials.size(), resourceHandle);
        } else {
            logger.info("Parsed %d materials", materials.size());
        }

        int numMeshes = aiScene.mNumMeshes();
        PointerBuffer aiMeshes = aiScene.mMeshes();
        List<Mesh> meshes = new ArrayList<>(numMeshes);
        for (int i = 0; i < numMeshes; i++) {
            logger.info("Parsing mesh %d", i);
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
            Mesh mesh = parseMesh(aiMesh, materials);
            meshes.add(mesh);
        }

        aiReleaseImport(aiScene);

        return new Model(meshes, new Matrix4f().identity());
    }

    @Nonnull
    private Mesh parseMesh(AIMesh aiMesh, List<Material> materials) {
        Material material;
        int materialIndex = aiMesh.mMaterialIndex();
        if (materialIndex >= 0 && materialIndex < materials.size()) {
            material = materials.get(materialIndex);
            logger.info("Mesh uses material with index %d", materialIndex);
        } else {
            material = new Material(null, new Vector4f(1, 1, 1, 1));
            logger.info("Mesh references an invalid material (%d) so it will use a default one", materialIndex);
        }

        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        AIVector3D.Buffer aiTextureCoords = aiMesh.mTextureCoords(0);
        AIColor4D.Buffer aiColors = aiMesh.mColors(0);
        int numVertices = aiMesh.mNumVertices();
        float[] vertices = new float[numVertices * 8];
        for (int i = 0; i < numVertices; i++) {
            AIVector3D aiVertex = aiVertices.get(i);
            vertices[(i * 8) + 0] = aiVertex.x();
            vertices[(i * 8) + 1] = aiVertex.y();
            vertices[(i * 8) + 2] = aiVertex.z();

            if (aiColors != null) {
                AIColor4D aiColor = aiColors.get(i);
                vertices[(i * 8) + 3] = aiColor.r();
                vertices[(i * 8) + 4] = aiColor.g();
                vertices[(i * 8) + 5] = aiColor.b();
            } else {
                vertices[(i * 8) + 3] = 1.0f;
                vertices[(i * 8) + 4] = 1.0f;
                vertices[(i * 8) + 5] = 1.0f;
            }

            if (aiTextureCoords != null) {
                AIVector3D aiTextureCoord = aiTextureCoords.get(i);
                vertices[(i * 8) + 6] = aiTextureCoord.x();
                vertices[(i * 8) + 7] = aiTextureCoord.y();
            } else {
                vertices[(i * 8) + 6] = 0.0f;
                vertices[(i * 8) + 7] = 0.0f;
            }
        }
        logger.info("Mesh has %d vertices", numVertices);

        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer faces = aiMesh.mFaces();
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(numFaces * 3);
        for (int i = 0; i < faces.limit(); i++) {
            AIFace aiFace = faces.get(i);
            // Only process triangles
            if (aiFace.mNumIndices() == 3) {
                IntBuffer aiIndices = aiFace.mIndices();
                indexBuffer.put(aiIndices.get(0));
                indexBuffer.put(aiIndices.get(1));
                indexBuffer.put(aiIndices.get(2));
            }
        }
        indexBuffer.flip();
        logger.info("Mesh has %d indices", indexBuffer.limit());

        VertexDataStruct.Buffer vertexBuffer = new VertexDataStruct.Buffer(vertices);

        MeshData meshData = new MeshData(
                renderer.getGraphicsQueue(),
                renderer.getGraphicsCommandPool(),
                vertexBuffer, indexBuffer
        );

        Mesh mesh = new Mesh(meshData, material);
        vertexBuffer.dispose();
        disposables.add(meshData);

        return mesh;
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
                FileHandle resource = new FileHandle(diffuseTexturePath);
                diffuseTexture = loadTexture(resource);
                logger.info("Material has diffuse texture: %s", resource);
            }

            Vector4f diffuseColor = new Vector4f();
            int result = aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color);
            if (result == 0) {
                diffuseColor = new Vector4f(color.r(), color.g(), color.b(), color.a());
                logger.info("Material has diffuse color: %s", diffuseColor);
            }
            return new Material(diffuseTexture, diffuseColor);
        }
    }

    @Override
    @Nonnull
    public Texture loadTexture(@Nonnull FileHandle resourceHandle) {
        logger.info("Loading texture: " + resourceHandle);
        return renderer.createTexture(resourceHandle.uri());
    }

    @Nonnull
    private AIScene loadAssimpScene(@Nonnull FileHandle resourceHandle) {
        AIFileIO fileIO = AIFileIO.create()
                .OpenProc((pFileIo, pFileName, openMode) -> {
                    ByteBuffer data = fileLoader.loadFile(new FileHandle(memUTF8(pFileName))).dataBuffer();
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
