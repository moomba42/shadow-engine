package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.backend.Disposable;
import org.lwjgl.vulkan.*;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class Mesh implements Disposable {
    private final VkBuffer vertexBuffer;
    private final int indexCount;
    private final VkBuffer indexBuffer;
    private final VkDevice logicalDevice;

    public Mesh(@Nonnull VkQueue transferQueue,
                @Nonnull VkCommandPool transferCommandPool,
                @Nonnull List<Vertex> vertices,
                @Nonnull List<Integer> indices) {
        this.indexCount = indices.size();
        this.logicalDevice = transferQueue.getDevice();
        this.vertexBuffer = createVertexBuffer(vertices, transferQueue, transferCommandPool);
        this.indexBuffer = createIndexBuffer(indices, transferQueue, transferCommandPool);
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

    private static VkBuffer createVertexBuffer(List<Vertex> vertices, VkQueue transferQueue, VkCommandPool transferCommandPool) {
        try (VulkanSession vk = new VulkanSession()) {
            long bufferSize = (long) Vertex.BYTES * vertices.size();
            VkDevice logicalDevice = transferQueue.getDevice();
            VkBuffer stagingBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            assert stagingBuffer.memory() != null;

            FloatBuffer stagingBufferView = vk.mapMemoryFloat(logicalDevice, stagingBuffer.memory(), 0, bufferSize, 0);
            for (int i = 0; i < vertices.size(); i++) {
                stagingBufferView.put(((i*6) + 0), vertices.get(i).position().x());
                stagingBufferView.put(((i*6) + 1), vertices.get(i).position().y());
                stagingBufferView.put(((i*6) + 2), vertices.get(i).position().z());
                stagingBufferView.put(((i*6) + 3), vertices.get(i).color().x());
                stagingBufferView.put(((i*6) + 4), vertices.get(i).color().y());
                stagingBufferView.put(((i*6) + 5), vertices.get(i).color().z());
            }
            vk.unmapMemory(logicalDevice, stagingBuffer.memory());

            VkBuffer vertexBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(transferQueue, transferCommandPool, stagingBuffer, vertexBuffer, bufferSize);
            vk.destroyBuffer(logicalDevice, stagingBuffer, null);
            vk.freeMemory(logicalDevice, stagingBuffer.memory(), null);

            return vertexBuffer;
        }
    }

    private static VkBuffer createIndexBuffer(List<Integer> indices, VkQueue transferQueue, VkCommandPool transferCommandPool) {
        try (VulkanSession vk = new VulkanSession()) {
            long bufferSize = (long) Integer.BYTES * indices.size();
            VkDevice logicalDevice = transferQueue.getDevice();
            VkBuffer stagingBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            assert stagingBuffer.memory() != null;

            IntBuffer stagingBufferView = vk.mapMemoryInt(logicalDevice, stagingBuffer.memory(), 0, bufferSize, 0);
            for (int i = 0; i < indices.size(); i++) {
                stagingBufferView.put(i, indices.get(i));
            }
            vk.unmapMemory(logicalDevice, stagingBuffer.memory());

            VkBuffer indexBuffer = createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            copyBuffer(transferQueue, transferCommandPool, stagingBuffer, indexBuffer, bufferSize);
            vk.destroyBuffer(logicalDevice, stagingBuffer, null);
            vk.freeMemory(logicalDevice, stagingBuffer.memory(), null);

            return indexBuffer;
        }
    }

    private static VkBuffer createBuffer(VkDevice logicalDevice, long size, int usage, int memoryFlags) {
        try(VulkanSession vk = new VulkanSession()) {
            var bufferCreateInfo = VkBufferCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            VkBuffer buffer = vk.createBuffer(logicalDevice, bufferCreateInfo);

            var memoryRequirements = vk.getBufferMemoryRequirements(logicalDevice, buffer);
            var memoryProperties = vk.getPhysicalDeviceMemoryProperties(logicalDevice.getPhysicalDevice());
            int memoryTypeIndex = -1;
            for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
                if ((memoryRequirements.memoryTypeBits() & (1 << i)) != 0 &&
                    (memoryProperties.memoryTypes(i).propertyFlags() & memoryFlags) == memoryFlags) {
                    memoryTypeIndex = i;
                    break;
                }
            }

            var memoryAllocateInfo = VkMemoryAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(memoryTypeIndex);
            VkDeviceMemory bufferMemory = vk.allocateMemory(logicalDevice, memoryAllocateInfo);

            vk.bindBufferMemory(logicalDevice, buffer, bufferMemory, 0);

            return new VkBuffer(buffer.address(), bufferMemory);
        }
    }

    private static void copyBuffer(VkQueue transferQueue, VkCommandPool transferCommandPool, VkBuffer srcBuffer, VkBuffer dstBuffer, long bufferSize) {
        try(VulkanSession vk = new VulkanSession()) {
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
        try(VulkanSession vk = new VulkanSession()) {
            vk.destroyBuffer(logicalDevice, vertexBuffer, null);
            vk.destroyBuffer(logicalDevice, indexBuffer, null);

            assert vertexBuffer.memory() != null;
            vk.freeMemory(logicalDevice, vertexBuffer.memory(), null);
            assert indexBuffer.memory() != null;
            vk.freeMemory(logicalDevice, indexBuffer.memory(), null);
        }
    }
}
