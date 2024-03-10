package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.backend.Disposable;
import org.lwjgl.vulkan.VkBuffer;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceMemory;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class Mesh implements Disposable {
    private final int vertexCount;
    private final VkBuffer vertexBuffer;
    private final VkDeviceMemory vertexBufferMemory;
    private final VkDevice logicalDevice;

    public Mesh(@Nonnull VkDevice logicalDevice,
                @Nonnull VkPhysicalDevice physicalDevice,
                @Nonnull List<Vertex> vertices) {
        this.vertexCount = vertices.size();
        this.logicalDevice = logicalDevice;

        try (VulkanSession vk = new VulkanSession()) {
            var bufferCreateInfo = VkBufferCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .size((long) Vertex.BYTES * vertices.size())
                    .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            this.vertexBuffer = vk.createBuffer(logicalDevice, bufferCreateInfo);

            var memoryRequirements = vk.getBufferMemoryRequirements(logicalDevice, vertexBuffer);
            var memoryRequiredFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | // Visible to the CPU
                                      VK_MEMORY_PROPERTY_HOST_COHERENT_BIT; // Data should be immediately put into the buffer to avoid flushing.
            var memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice);
            int memoryTypeIndex = -1;
            for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
                if ((memoryRequirements.memoryTypeBits() & (1 << i)) != 0 &&
                    (memoryProperties.memoryTypes(i).propertyFlags() & memoryRequiredFlags) == memoryRequiredFlags) {
                    memoryTypeIndex = i;
                    break;
                }
            }

            var memoryAllocateInfo = VkMemoryAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(memoryTypeIndex);
            this.vertexBufferMemory = vk.allocateMemory(logicalDevice, memoryAllocateInfo);

            vk.bindBufferMemory(logicalDevice, vertexBuffer, vertexBufferMemory, 0);

            FloatBuffer vertexBufferView = vk.mapMemoryFloat(logicalDevice, vertexBufferMemory, 0, memoryRequirements.size(), 0);
            for (Vertex vertex : vertices) {
                vertexBufferView.put(vertex.position().x());
                vertexBufferView.put(vertex.position().y());
                vertexBufferView.put(vertex.position().z());
                vertexBufferView.put(vertex.color().x());
                vertexBufferView.put(vertex.color().y());
                vertexBufferView.put(vertex.color().z());
            }
            vertexBufferView.flip();

            vk.unmapMemory(logicalDevice, vertexBufferMemory);
        }
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public VkBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    @Override
    public void dispose() {
        vkDestroyBuffer(logicalDevice, vertexBuffer.address(), null);
        vkFreeMemory(logicalDevice, vertexBufferMemory.address(), null);
    }
}
