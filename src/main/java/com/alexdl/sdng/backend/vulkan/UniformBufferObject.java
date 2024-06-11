package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.backend.Disposable;
import com.alexdl.sdng.backend.vulkan.structs.MemoryBlock;
import org.lwjgl.vulkan.*;

import javax.annotation.Nonnull;

import java.nio.LongBuffer;
import java.util.List;

import static com.alexdl.sdng.backend.vulkan.VulkanUtils.createUniformBuffers;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.VK10.*;

public class UniformBufferObject<Data extends MemoryBlock<Data>> implements Disposable {
    protected final VkDevice logicalDevice;
    protected final VkDescriptorSetLayout descriptorSetLayout;
    protected final VkDescriptorPool descriptorPool;
    protected final List<VkDescriptorSet> descriptorSets;
    protected final List<VkBuffer> buffers;
    protected final int instanceCount;
    protected final int instanceSizeBytes;

    public UniformBufferObject(@Nonnull VkDevice logicalDevice, int instanceCount, int instanceSizeBytes, int stageFlags) {
        this.logicalDevice = logicalDevice;
        this.instanceCount = instanceCount;
        this.instanceSizeBytes = instanceSizeBytes;

        try(VulkanSession vk = new VulkanSession()) {
            // Descriptor set layout
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(1, vk.stack());
            layoutBindings.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(stageFlags);

            VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pBindings(layoutBindings);

            this.descriptorSetLayout = vk.createDescriptorSetLayout(logicalDevice, layoutCreateInfo, null);

            // Descriptor pool
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(1, vk.stack());
            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(instanceCount);

            VkDescriptorPoolCreateInfo poolCreateInfo = VkDescriptorPoolCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .maxSets(instanceCount)
                    .pPoolSizes(poolSizes);

            this.descriptorPool = vk.createDescriptorPool(logicalDevice, poolCreateInfo, null);

            // Descriptor sets
            LongBuffer setLayouts = vk.stack().mallocLong(instanceCount);
            for (int i = 0; i < instanceCount; i++) {
                setLayouts.put(i, descriptorSetLayout.address());
            }

            VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .descriptorPool(descriptorPool.address())
                    .pSetLayouts(setLayouts);

            this.descriptorSets = vk.allocateDescriptorSets(logicalDevice, descriptorSetAllocateInfo);

            // Buffers
            this.buffers = createUniformBuffers(logicalDevice, instanceCount, instanceSizeBytes);

            // Connect descriptor sets to buffers
            for (int i = 0; i < instanceCount; i++) {
                VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.calloc(1, vk.stack())
                        .buffer(buffers.get(i).address())
                        .offset(0)
                        .range(instanceSizeBytes);

                VkWriteDescriptorSet.Buffer setWrites = VkWriteDescriptorSet.calloc(1, vk.stack())
                        .sType$Default()
                        .dstSet(descriptorSets.get(i).address())
                        .dstBinding(0)
                        .dstArrayElement(0)
                        .descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .pBufferInfo(bufferInfos);

                vk.updateDescriptorSets(logicalDevice, setWrites, null);
            }
        }
    }

    public void updateBuffer(int index, Data data) {
        assert data.size() == instanceSizeBytes;
        try(VulkanSession vk = new VulkanSession()) {
            VkDeviceMemory sceneMemory = buffers.get(index).memory();
            assert sceneMemory != null;
            long sceneTarget = vk.mapMemoryPointer(logicalDevice, sceneMemory, 0, instanceSizeBytes, 0);
            memCopy(data.address(), sceneTarget, instanceSizeBytes);
            vk.unmapMemory(logicalDevice, sceneMemory);
        }
    }

    public VkDescriptorSetLayout getDescriptorSetLayout() {
        return descriptorSetLayout;
    }

    public VkDescriptorSet getDescriptorSet(int index) {
        return descriptorSets.get(index);
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    @Override
    public void dispose() {
        for (VkBuffer buffer : buffers) {
            vkDestroyBuffer(logicalDevice, buffer.address(), null);
            if(buffer.memory() != null) {
                vkFreeMemory(logicalDevice, buffer.memory().address(), null);
            }
        }
        vkDestroyDescriptorPool(logicalDevice, descriptorPool.address(), null);
        vkDestroyDescriptorSetLayout(logicalDevice, descriptorSetLayout.address(), null);
    }
}
