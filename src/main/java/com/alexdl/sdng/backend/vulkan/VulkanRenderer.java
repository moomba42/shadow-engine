package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.Configuration;
import com.alexdl.sdng.File;
import com.alexdl.sdng.Renderer;
import com.alexdl.sdng.backend.vulkan.structs.EnvironmentDataStruct;
import com.alexdl.sdng.backend.vulkan.structs.MemoryBlockBuffer;
import com.alexdl.sdng.backend.vulkan.structs.ModelDataStruct;
import com.alexdl.sdng.backend.vulkan.structs.PushConstantStruct;
import com.alexdl.sdng.backend.vulkan.structs.SceneDataStruct;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GlfwWindow;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.enums.VkFormat;
import org.lwjgl.vulkan.enums.VkImageLayout;
import org.lwjgl.vulkan.enums.VkImageTiling;
import org.lwjgl.vulkan.enums.VkPresentModeKHR;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.alexdl.sdng.backend.vulkan.VulkanUtils.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer implements Renderer {
    private static final int MAX_CONCURRENT_FRAME_DRAWS = 2;
    private static final int MAX_OBJECTS = 10;
    private final VkInstance instance;
    private final VkSurfaceKHR surface;
    private final Long debugMessengerPointer;

    private final VkDevice logicalDevice;

    private final PushConstantStruct pushConstant;

    private final VkQueue graphicsQueue;
    private final VkQueue presentQueue;

    private final SwapchainImageConfig swapchainImageConfig;
    private final VkSwapchainKHR swapchain;

    private final VkDescriptorSetLayout descriptorSetLayout;
    private final VkDescriptorPool descriptorPool;
    private final VkPipelineLayout pipelineLayout;
    private final VkRenderPass renderPass;
    private final VkPipeline graphicsPipeline;
    private final VkCommandPool graphicsCommandPool;

    // For each swapchain image
    private final Image depthBufferImage;
    private final List<SwapchainImage> swapchainImages;
    private final List<VkFramebuffer> swapchainFramebuffers;
    private final List<VkCommandBuffer> swapchainCommandBuffers;
    private final List<VkBuffer> modelUniformBuffers;
    private final MemoryBlockBuffer<ModelDataStruct> modelUniformTransferSpace;
    private final List<VkDescriptorSet> descriptorSets;

    // Assets
    private final List<VkBuffer> sceneUniformBuffers; // For each swapchain image
    private final SceneDataStruct sceneData;

    // Environment
    private final EnvironmentDataStruct environmentData;
    private final UniformBufferObject<EnvironmentDataStruct> environmentUbo;

    // Textures
    private final List<Image> images;
    private final VkDescriptorSetLayout samplerSetLayout;
    private final VkDescriptorPool samplerDescriptorPool;
    private final VkSampler sampler;

    private final Set<Model> modelsToDraw;

    // For each frame
    private final List<VkFence> frameDrawFences;
    private final List<VkSemaphore> frameImageAvailableSemaphores;
    private final List<VkSemaphore> frameDrawSemaphores;

    private final Texture defaultTexture;

    private int currentFrame = 0;

    @Inject
    public VulkanRenderer(GlfwWindow window, Configuration configuration) {
        modelsToDraw = new HashSet<>();
        instance = createInstance(configuration.debuggingEnabled());
        surface = createSurface(instance, window.address());
        debugMessengerPointer = configuration.debuggingEnabled() ? createDebugMessenger(instance) : null;

        VkPhysicalDevice physicalDevice = findFirstSuitablePhysicalDevice(instance, surface);
        logicalDevice = createLogicalDevice(physicalDevice, surface);

        VkPresentModeKHR presentationMode = findBestPresentationMode(physicalDevice, surface);
        swapchainImageConfig = findBestSwapchainImageConfig(physicalDevice, surface, window.address());
        swapchain = createSwapchain(physicalDevice, logicalDevice, surface, swapchainImageConfig, presentationMode);
        swapchainImages = createSwapchainImageViews(logicalDevice, swapchain, swapchainImageConfig.format());

        QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
        graphicsQueue = findFirstQueueByFamily(logicalDevice, queueIndices.graphical());
        presentQueue = findFirstQueueByFamily(logicalDevice, queueIndices.surfaceSupporting());

        pushConstant = new PushConstantStruct();
        depthBufferImage = createDepthBufferImage(logicalDevice, swapchainImageConfig.extent().width(), swapchainImageConfig.extent().height());

        environmentData = new EnvironmentDataStruct(10);
        environmentData.setLightCount(2);
        environmentData.setLight(0, -3, 3, 4, 1, 0, 0, 6, 0f, 0.1f);
        environmentData.setLight(1, 3, -3, 0, 0, 1, 0, 6, 0, 0.1f);
        environmentUbo = new UniformBufferObject<>(logicalDevice, swapchainImages.size(), environmentData.size());

        descriptorSetLayout = createDescriptorSetLayout(logicalDevice);
        samplerSetLayout = createSamplerSetLayout(logicalDevice);
        pipelineLayout = createPipelineLayout(logicalDevice, List.of(descriptorSetLayout, samplerSetLayout, environmentUbo.getDescriptorSetLayout()));
        renderPass = createRenderPass(logicalDevice, swapchainImageConfig.format(), depthBufferImage.format());
        graphicsPipeline = createGraphicsPipeline(logicalDevice, swapchainImageConfig.extent(), pipelineLayout, renderPass);
        graphicsCommandPool = createCommandPool(logicalDevice, queueIndices.graphical());

        swapchainFramebuffers = createFramebuffers(logicalDevice, renderPass, swapchainImageConfig, swapchainImages, depthBufferImage);
        swapchainCommandBuffers = createCommandBuffers(logicalDevice, graphicsCommandPool, swapchainFramebuffers);

        Matrix4f projection = new Matrix4f()
                .perspective(
                        (float) Math.toRadians(45.0f),
                        (float) swapchainImageConfig.extent().width() / (float) swapchainImageConfig.extent().height(),
                        0.01f,
                        100.0f);
        projection.set(1, 1, projection.getRowColumn(1, 1) * -1);

        sceneData = new SceneDataStruct()
                .projection(projection)
                .view(new Matrix4f().lookAt(
                        new Vector3f(0.0f, 0.0f, 10.0f),
                        new Vector3f(0.0f, 0.0f, 0.0f),
                        new Vector3f(0.0f, 1.0f, 0.0f)));

        int minUniformBufferOffsetAlignment;
        try (VulkanSession vk = new VulkanSession()) {
            minUniformBufferOffsetAlignment = (int) vk.getPhysicalDeviceProperties(physicalDevice).limits().minUniformBufferOffsetAlignment();
        }

        modelUniformTransferSpace = new MemoryBlockBuffer<>(MAX_OBJECTS, new ModelDataStruct(), minUniformBufferOffsetAlignment);
        sceneUniformBuffers = createUniformBuffers(logicalDevice, swapchainImages.size(), sceneData.size());
        modelUniformBuffers = createUniformBuffers(logicalDevice, swapchainImages.size(), modelUniformTransferSpace.size());
        descriptorPool = createDescriptorPool(logicalDevice, sceneUniformBuffers.size(), modelUniformBuffers.size(), swapchainImages.size());
        samplerDescriptorPool = createSamplerDescriptorPool(logicalDevice, MAX_OBJECTS, MAX_OBJECTS);
        descriptorSets = createDescriptorSets(logicalDevice, descriptorPool, descriptorSetLayout, swapchainImages.size());
        connectDescriptorSetsToUniformBuffers(logicalDevice, descriptorSets, sceneUniformBuffers, sceneData.size(), modelUniformBuffers, modelUniformTransferSpace.elementSize());

        images = new ArrayList<>(10);
        sampler = createTextureSampler(logicalDevice);

        frameDrawFences = new ArrayList<>(MAX_CONCURRENT_FRAME_DRAWS);
        frameImageAvailableSemaphores = new ArrayList<>(MAX_CONCURRENT_FRAME_DRAWS);
        frameDrawSemaphores = new ArrayList<>(MAX_CONCURRENT_FRAME_DRAWS);
        try (VulkanSession vk = new VulkanSession()) {
            for (int i = 0; i < MAX_CONCURRENT_FRAME_DRAWS; i++) {
                frameDrawFences.add(i, vk.createFence(logicalDevice, VK_FENCE_CREATE_SIGNALED_BIT));
                frameImageAvailableSemaphores.add(i, vk.createSemaphore(logicalDevice));
                frameDrawSemaphores.add(i, vk.createSemaphore(logicalDevice));
            }
        }

        byte[] defaultTextureData = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAAXNSR0IArs4c6QAAAANQTFRF////p8QbyAAAAA1JREFUGJVjYBgFyAAAARAAATPJ8WoAAAAASUVORK5CYII=");
        defaultTexture = createTexture(new File(null, BufferUtils.createByteBuffer(defaultTextureData.length).put(defaultTextureData).flip()));
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public VkCommandPool getGraphicsCommandPool() {
        return graphicsCommandPool;
    }

    @Override
    public void updatePushConstant(@Nonnull Matrix4f transform) {
        pushConstant.transform(transform);
    }

    @Override
    public void queueModel(@Nonnull Model model) {
        modelsToDraw.add(model);
    }

    @Override
    public void draw() {
        try (VulkanSession vk = new VulkanSession()) {
            // Wait for previous frame
            vkWaitForFences(logicalDevice, frameDrawFences.get(currentFrame).address(), true, Integer.MAX_VALUE);
            vkResetFences(logicalDevice, frameDrawFences.get(currentFrame).address());

            // Get next image
            IntBuffer imageIndexPointer = vk.stack().mallocInt(1);
            vkAcquireNextImageKHR(logicalDevice, swapchain.address(), Long.MAX_VALUE, frameImageAvailableSemaphores.get(currentFrame).address(), VK_NULL_HANDLE, imageIndexPointer);
            int imageIndex = imageIndexPointer.get(0);

            recordCommands(imageIndex);
            updateUniforms(imageIndex);

            // Submit
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(vk.stack())
                    .sType$Default()
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(vk.stack().longs(frameImageAvailableSemaphores.get(currentFrame).address()))
                    .pWaitDstStageMask(vk.stack().ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(vk.stack().pointers(swapchainCommandBuffers.get(imageIndex)))
                    .pSignalSemaphores(vk.stack().longs(frameDrawSemaphores.get(currentFrame).address()));
            throwIfFailed(vkQueueSubmit(graphicsQueue, submitInfo, frameDrawFences.get(currentFrame).address()));

            // Present
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(vk.stack())
                    .sType$Default()
                    .pWaitSemaphores(vk.stack().longs(frameDrawSemaphores.get(currentFrame).address()))
                    .swapchainCount(1)
                    .pSwapchains(vk.stack().longs(swapchain.address()))
                    .pImageIndices(vk.stack().ints(imageIndex));
            throwIfFailed(vkQueuePresentKHR(presentQueue, presentInfo));

            // Increment current frame
            currentFrame = (currentFrame + 1) % MAX_CONCURRENT_FRAME_DRAWS;

            modelsToDraw.clear();
        }
    }

    @Override
    public void dispose() {
        throwIfFailed(vkDeviceWaitIdle(logicalDevice));

        vkDestroyDescriptorPool(logicalDevice, samplerDescriptorPool.address(), null);
        vkDestroyDescriptorSetLayout(logicalDevice, samplerSetLayout.address(), null);

        vkDestroySampler(logicalDevice, sampler.address(), null);

        for (Image image : images) {
            vkDestroyImageView(logicalDevice, image.view().address(), null);
            vkDestroyImage(logicalDevice, image.image().address(), null);
            vkFreeMemory(logicalDevice, image.memory().address(), null);
        }

        vkDestroyImageView(logicalDevice, depthBufferImage.view().address(), null);
        vkDestroyImage(logicalDevice, depthBufferImage.image().address(), null);
        vkFreeMemory(logicalDevice, depthBufferImage.memory().address(), null);

        modelUniformTransferSpace.dispose();
        sceneData.dispose();

        vkDestroyDescriptorPool(logicalDevice, descriptorPool.address(), null);
        vkDestroyDescriptorSetLayout(logicalDevice, descriptorSetLayout.address(), null);
        for (VkBuffer buffer : modelUniformBuffers) {
            vkDestroyBuffer(logicalDevice, buffer.address(), null);
            if (buffer.memory() != null) {
                vkFreeMemory(logicalDevice, buffer.memory().address(), null);
            }
        }
        for (VkBuffer buffer : sceneUniformBuffers) {
            vkDestroyBuffer(logicalDevice, buffer.address(), null);
            if (buffer.memory() != null) {
                vkFreeMemory(logicalDevice, buffer.memory().address(), null);
            }
        }
        frameDrawFences.forEach(fence -> vkDestroyFence(logicalDevice, fence.address(), null));
        frameImageAvailableSemaphores.forEach(semaphore -> vkDestroySemaphore(logicalDevice, semaphore.address(), null));
        frameDrawSemaphores.forEach(semaphore -> vkDestroySemaphore(logicalDevice, semaphore.address(), null));
        vkDestroyCommandPool(logicalDevice, graphicsCommandPool.address(), null);
        for (VkFramebuffer framebuffer : swapchainFramebuffers) {
            vkDestroyFramebuffer(logicalDevice, framebuffer.address(), null);
        }
        swapchainImageConfig.dispose();
        vkDestroyPipeline(logicalDevice, graphicsPipeline.address(), null);
        vkDestroyPipelineLayout(logicalDevice, pipelineLayout.address(), null);
        vkDestroyRenderPass(logicalDevice, renderPass.address(), null);
        for (SwapchainImage swapchainImage : swapchainImages) {
            vkDestroyImageView(logicalDevice, swapchainImage.view().address(), null);
        }
        vkDestroySwapchainKHR(logicalDevice, swapchain.address(), null);
        vkDestroySurfaceKHR(instance, surface.address(), null);
        vkDestroyDevice(logicalDevice, null);
        if (debugMessengerPointer != null) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessengerPointer, null);
        }
        vkDestroyInstance(instance, null);
    }

    private void updateUniforms(int imageIndex) {
        try (VulkanSession vk = new VulkanSession()) {
            // Scene
            VkDeviceMemory sceneMemory = sceneUniformBuffers.get(imageIndex).memory();
            assert sceneMemory != null;
            long sceneTarget = vk.mapMemoryPointer(logicalDevice, sceneMemory, 0, sceneData.size(), 0);
            memCopy(sceneData.address(), sceneTarget, sceneData.size());
            vk.unmapMemory(logicalDevice, sceneMemory);

            // Environment
            environmentUbo.updateBuffer(imageIndex, environmentData);

            // Models
            int modelCounter = 0;
            for (Model model : modelsToDraw) {
                modelUniformTransferSpace.get(modelCounter).modelTransform(model.transform());
                // TODO: Replace this with actual normal matrix
                modelUniformTransferSpace.get(modelCounter).normalTransform(new Matrix4f(model.transform()).invert().transpose());
                modelCounter = modelCounter + 1;
            }
            VkDeviceMemory modelMemory = modelUniformBuffers.get(imageIndex).memory();
            assert modelMemory != null;
            long modelBufferSizeBytes = modelUniformTransferSpace.size();
            long modelTarget = vk.mapMemoryPointer(logicalDevice, modelMemory, 0, modelBufferSizeBytes, 0);
            memCopy(modelUniformTransferSpace.address(), modelTarget, modelBufferSizeBytes);
            vk.unmapMemory(logicalDevice, modelMemory);
        }
    }

    private void recordCommands(int imageIndex) {
        try (VulkanSession vk = new VulkanSession()) {
            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(vk.stack())
                    .sType$Default();

            VkClearValue.Buffer clearValues = VkClearValue.calloc(2);
            clearValues.get(0).color()
                    .float32(0, 0.0f)
                    .float32(1, 0.0f)
                    .float32(2, 0.0f)
                    .float32(3, 1.0f);
            clearValues.get(1).depthStencil().set(1.0f, 0);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(vk.stack())
                    .sType$Default()
                    .renderPass(renderPass.address())
                    .pClearValues(clearValues);
            renderPassBeginInfo.renderArea().extent(swapchainImageConfig.extent()).offset().set(0, 0);
            renderPassBeginInfo.framebuffer(swapchainFramebuffers.get(imageIndex).address());

            VkCommandBuffer commandBuffer = swapchainCommandBuffers.get(imageIndex);

            throwIfFailed(vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo));
            vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.address());

            // TODO: Doe something about the push constant
            // nvkCmdPushConstants(commandBuffer, pipelineLayout.address(), VK_SHADER_STAGE_VERTEX_BIT, 0, PushConstantStruct.SIZE, pushConstant.address());

            int modelIndex = 0;
            int meshIndex = 0;
            for (Model model : modelsToDraw) {
                int dynamicOffset = modelIndex * modelUniformTransferSpace.elementSize();
                for (Mesh mesh : model.meshes()) {
                    vkCmdBindVertexBuffers(
                            commandBuffer,
                            0,
                            vk.stack().longs(mesh.data().getVertexBuffer().address()),
                            vk.stack().longs(0)
                    );
                    vkCmdBindIndexBuffer(
                            commandBuffer,
                            mesh.data().getIndexBuffer().address(),
                            0,
                            VK_INDEX_TYPE_UINT32
                    );


                    Texture diffuseTexture = mesh.material().diffuse();
                    if(diffuseTexture == null) {
                        diffuseTexture = defaultTexture;
                    }
                    vkCmdBindDescriptorSets(
                            commandBuffer,
                            VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayout.address(),
                            0,
                            vk.stack().longs(
                                    descriptorSets.get(imageIndex).address(),
                                    diffuseTexture.descriptorSet().address(),
                                    environmentUbo.getDescriptorSet(imageIndex).address()
                            ),
                            vk.stack().ints(dynamicOffset)
                    );

                    vkCmdDrawIndexed(commandBuffer, mesh.data().getIndexCount(), 1, 0, 0, 0);
                    meshIndex = meshIndex + 1;
                }
                modelIndex = modelIndex + 1;
            }

            vkCmdEndRenderPass(commandBuffer);
            throwIfFailed(vkEndCommandBuffer(commandBuffer));
        }
    }

    private Image createTextureImage(ByteBuffer file) {
        try (VulkanSession vk = new VulkanSession()) {
            IntBuffer widthBuffer = vk.stack().mallocInt(1);
            IntBuffer heightBuffer = vk.stack().mallocInt(1);
            IntBuffer channelsBuffer = vk.stack().mallocInt(1);

            ByteBuffer imageData = stbi_load_from_memory(file, widthBuffer, heightBuffer, channelsBuffer, STBI_rgb_alpha);
            if (imageData == null) {
                throw new RuntimeException("Failed to parse image as resource");
            }
            int width = widthBuffer.get(0);
            int height = heightBuffer.get(0);
            int imageDataSize = width * height * STBI_rgb_alpha;

            VkBuffer imageStagingBuffer = createBuffer(logicalDevice, imageDataSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            assert imageStagingBuffer.memory() != null;
            long stagingBufferMappedMemoryAddress = vk.mapMemoryPointer(logicalDevice, imageStagingBuffer.memory(), 0, imageDataSize, 0);
            memCopy(memAddress(imageData), stagingBufferMappedMemoryAddress, imageDataSize);
            vk.unmapMemory(logicalDevice, imageStagingBuffer.memory());

            stbi_image_free(imageData);

            Image image = createImage(
                    logicalDevice,
                    width, height,
                    VkFormat.VK_FORMAT_R8G8B8A8_UNORM,
                    VkImageTiling.VK_IMAGE_TILING_OPTIMAL,
                    new VkImageUsageFlags(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT),
                    new VkMemoryPropertyFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
                    new VkImageAspectFlags(VK_IMAGE_ASPECT_COLOR_BIT)
            );

            transitionImageLayout(logicalDevice, graphicsQueue, graphicsCommandPool, image.image(), VkImageLayout.VK_IMAGE_LAYOUT_UNDEFINED, VkImageLayout.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            copyColorImageBuffer(logicalDevice, graphicsQueue, graphicsCommandPool, imageStagingBuffer, image.image(), width, height);
            transitionImageLayout(logicalDevice, graphicsQueue, graphicsCommandPool, image.image(), VkImageLayout.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VkImageLayout.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            vk.destroyBuffer(logicalDevice, imageStagingBuffer, null);
            vk.freeMemory(logicalDevice, imageStagingBuffer.memory(), null);

            return image;
        }
    }

    private VkDescriptorSet createTextureDescriptor(VkImageView textureImageView) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .descriptorPool(samplerDescriptorPool.address())
                    .pSetLayouts(vk.stack().longs(samplerSetLayout.address()));

            VkDescriptorSet descriptorSet = vk.allocateDescriptorSets(logicalDevice, descriptorSetAllocateInfo).getFirst();

            VkDescriptorImageInfo.Buffer descriptorImageInfo = VkDescriptorImageInfo.calloc(1, vk.stack())
                    .sampler(sampler.address())
                    .imageView(textureImageView.address())
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL); // image layout when in use
            VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.calloc(1, vk.stack())
                    .sType$Default()
                    .dstSet(descriptorSet.address())
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImageInfo(descriptorImageInfo);

            vk.updateDescriptorSets(logicalDevice, writeDescriptorSet, null);
            return descriptorSet;
        }
    }

    public @Nonnull Texture createTexture(@Nonnull File file) {
        Image image = createTextureImage(file.dataBuffer());
        images.add(image);
        VkDescriptorSet descriptorSet = createTextureDescriptor(image.view());
        return new Texture(
                descriptorSet,
                image
        );
    }

}
