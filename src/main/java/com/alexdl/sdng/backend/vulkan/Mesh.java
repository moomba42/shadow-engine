package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.backend.Disposable;
import com.alexdl.sdng.backend.vulkan.structs.VertexData;
import org.joml.Matrix4f;
import org.lwjgl.vulkan.VkBuffer;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPool;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import javax.annotation.Nonnull;
import java.nio.IntBuffer;

import static com.alexdl.sdng.backend.vulkan.VulkanUtils.createBuffer;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.VK10.*;

public class Mesh implements Disposable {
    private final VkBuffer vertexBuffer;
    private final int indexCount;
    private final VkBuffer indexBuffer;
    private final VkDevice logicalDevice;
    private final Matrix4f transform;

    public Mesh(@Nonnull VkQueue transferQueue,
                @Nonnull VkCommandPool transferCommandPool,
                @Nonnull VertexData.Buffer vertexData,
                @Nonnull IntBuffer indexData) {
        this.indexCount = indexData.limit();
        this.logicalDevice = transferQueue.getDevice();
        this.vertexBuffer = createVertexBuffer(vertexData, transferQueue, transferCommandPool);
        this.indexBuffer = createIndexBuffer(indexData, transferQueue, transferCommandPool);
        this.transform = new Matrix4f().identity();
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

    private static VkBuffer createVertexBuffer(VertexData.Buffer vertexData, VkQueue transferQueue, VkCommandPool transferCommandPool) {
        try (VulkanSession vk = new VulkanSession()) {
            long bufferSize = (long) vertexData.sizeof() * vertexData.limit();
            VkDevice logicalDevice = transferQueue.getDevice();
            VkBuffer stagingBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            assert stagingBuffer.memory() != null;

            long stagingBufferMappedMemoryAddress = vk.mapMemoryPointer(logicalDevice, stagingBuffer.memory(), 0, bufferSize, 0);
            memCopy(vertexData.address(), stagingBufferMappedMemoryAddress, bufferSize);
            vk.unmapMemory(logicalDevice, stagingBuffer.memory());

            VkBuffer vertexBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(transferQueue, transferCommandPool, stagingBuffer, vertexBuffer, bufferSize);
            vk.destroyBuffer(logicalDevice, stagingBuffer, null);
            vk.freeMemory(logicalDevice, stagingBuffer.memory(), null);

            return vertexBuffer;
        }
    }

    private static VkBuffer createIndexBuffer(IntBuffer indexData, VkQueue transferQueue, VkCommandPool transferCommandPool) {
        try (VulkanSession vk = new VulkanSession()) {
            long bufferSize = (long) Integer.BYTES * indexData.limit();
            VkDevice logicalDevice = transferQueue.getDevice();
            VkBuffer stagingBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            assert stagingBuffer.memory() != null;

            long stagingBufferMappedMemoryAddress = vk.mapMemoryPointer(logicalDevice, stagingBuffer.memory(), 0, bufferSize, 0);
            memCopy(memAddress(indexData), stagingBufferMappedMemoryAddress, bufferSize);
            vk.unmapMemory(logicalDevice, stagingBuffer.memory());

            VkBuffer indexBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(transferQueue, transferCommandPool, stagingBuffer, indexBuffer, bufferSize);
            vk.destroyBuffer(logicalDevice, stagingBuffer, null);
            vk.freeMemory(logicalDevice, stagingBuffer.memory(), null);

            return indexBuffer;
        }
    }

    private static void copyBuffer(VkQueue transferQueue, VkCommandPool transferCommandPool, VkBuffer srcBuffer, VkBuffer dstBuffer, long bufferSize) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDevice logicalDevice = transferQueue.getDevice();
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .commandPool(transferCommandPool.address())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            VkCommandBuffer transferCommandBuffer = vk.allocateCommandBuffers(logicalDevice, commandBufferAllocateInfo).getFirst();

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(vk.stack())
                    .sType$Default()
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vk.beginCommandBuffer(transferCommandBuffer, beginInfo);

            VkBufferCopy.Buffer bufferRegionCopy = VkBufferCopy.calloc(1, vk.stack())
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(bufferSize);
            vk.cmdCopyBuffer(transferCommandBuffer, srcBuffer, dstBuffer, bufferRegionCopy);
            vk.endCommandBuffer(transferCommandBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(vk.stack())
                    .sType$Default()
                    .pCommandBuffers(vk.stack().pointers(transferCommandBuffer));
            vk.queueSubmit(transferQueue, submitInfo, null);

            vk.queueWaitIdle(transferQueue);
            vk.freeCommandBuffers(logicalDevice, transferCommandPool, transferCommandBuffer);
        }
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
