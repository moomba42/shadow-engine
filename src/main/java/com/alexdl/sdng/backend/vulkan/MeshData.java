package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.backend.Disposable;
import com.alexdl.sdng.backend.vulkan.structs.VertexDataStruct;
import org.joml.Matrix4f;
import org.lwjgl.vulkan.VkBuffer;
import org.lwjgl.vulkan.VkCommandPool;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import javax.annotation.Nonnull;
import java.nio.IntBuffer;

import static com.alexdl.sdng.backend.vulkan.VulkanUtils.*;

public class MeshData implements Disposable {
    private final VkBuffer vertexBuffer;
    private final int indexCount;
    private final VkBuffer indexBuffer;
    private final VkDevice logicalDevice;
    private final Matrix4f transform;
    private final int textureId;

    public MeshData(@Nonnull VkQueue transferQueue,
                    @Nonnull VkCommandPool transferCommandPool,
                    @Nonnull VertexDataStruct.Buffer vertexData,
                    @Nonnull IntBuffer indexData,
                    int textureId) {
        this.indexCount = indexData.limit();
        this.logicalDevice = transferQueue.getDevice();
        this.vertexBuffer = createVertexBuffer(vertexData, transferQueue, transferCommandPool);
        this.indexBuffer = createIndexBuffer(indexData, transferQueue, transferCommandPool);
        this.transform = new Matrix4f().identity();
        this.textureId = textureId;
    }

    public VkBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public VkBuffer getIndexBuffer() {
        return indexBuffer;
    }

    public Matrix4f getTransform() {
        return transform;
    }

    public int getTextureId() {
        return textureId;
    }

    @Override
    public void dispose() {
        try (VulkanSession vk = new VulkanSession()) {
            vk.destroyBuffer(logicalDevice, vertexBuffer, null);
            vk.destroyBuffer(logicalDevice, indexBuffer, null);

            assert vertexBuffer.memory() != null;
            vk.freeMemory(logicalDevice, vertexBuffer.memory(), null);
            assert indexBuffer.memory() != null;
            vk.freeMemory(logicalDevice, indexBuffer.memory(), null);
        }
    }
}
