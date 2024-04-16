package com.alexdl.sdng.backend.vulkan;

import com.alexdl.sdng.Configuration;
import com.alexdl.sdng.backend.Disposable;
import com.alexdl.sdng.backend.glfw.GLFWRuntimeException;
import com.alexdl.sdng.backend.glfw.GlfwWindow;
import com.alexdl.sdng.backend.vulkan.structs.MemoryBlockBuffer;
import com.alexdl.sdng.backend.vulkan.structs.ModelDataStruct;
import com.alexdl.sdng.backend.vulkan.structs.PushConstantStruct;
import com.alexdl.sdng.backend.vulkan.structs.SceneDataStruct;
import com.alexdl.sdng.backend.vulkan.structs.VertexDataStruct;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.enums.VkColorSpaceKHR;
import org.lwjgl.vulkan.enums.VkFormat;
import org.lwjgl.vulkan.enums.VkImageTiling;
import org.lwjgl.vulkan.enums.VkPresentModeKHR;
import org.lwjgl.vulkan.enums.VkSharingMode;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alexdl.sdng.backend.vulkan.VulkanUtils.*;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer implements Disposable {
    private static final int MAX_CONCURRENT_FRAME_DRAWS = 2;
    private static final int MAX_OBJECTS = 10;
    private final VkInstance instance;
    private final VkSurfaceKHR surface;
    private final Long debugMessengerPointer;

    private final VkDevice logicalDevice;

    private final SceneDataStruct sceneData;
    private final List<VertexDataStruct.Buffer> meshData;
    private final List<Mesh> meshes;

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
    private final List<VkBuffer> sceneUniformBuffers;
    private final List<VkBuffer> modelUniformBuffers;
    private final MemoryBlockBuffer<ModelDataStruct> modelUniformTransferSpace;
    private final List<VkDescriptorSet> descriptorSets;

    // For each frame
    private final List<VkFence> frameDrawFences;
    private final List<VkSemaphore> frameImageAvailableSemaphores;
    private final List<VkSemaphore> frameDrawSemaphores;

    private int currentFrame = 0;

    public VulkanRenderer(GlfwWindow window, Configuration configuration) {
        instance = createInstance(configuration.debuggingEnabled());
        surface = createSurface(instance, window);
        debugMessengerPointer = configuration.debuggingEnabled() ? createDebugMessenger(instance) : null;

        VkPhysicalDevice physicalDevice = findFirstSuitablePhysicalDevice(instance, surface);
        logicalDevice = createLogicalDevice(physicalDevice, surface);

        VkPresentModeKHR presentationMode = findBestPresentationMode(physicalDevice, surface);
        swapchainImageConfig = findBestSwapchainImageConfig(physicalDevice, surface, window);
        swapchain = createSwapchain(physicalDevice, logicalDevice, surface, swapchainImageConfig, presentationMode);
        swapchainImages = createSwapchainImageViews(logicalDevice, swapchain, swapchainImageConfig.format());

        QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
        graphicsQueue = findFirstQueueByFamily(logicalDevice, queueIndices.graphical());
        presentQueue = findFirstQueueByFamily(logicalDevice, queueIndices.surfaceSupporting());

        pushConstant = new PushConstantStruct();
        depthBufferImage = createDepthBufferImage(logicalDevice, swapchainImageConfig.extent().width(), swapchainImageConfig.extent().height());

        descriptorSetLayout = createDescriptorSetLayout(logicalDevice);
        pipelineLayout = createPipelineLayout(logicalDevice, List.of(descriptorSetLayout));
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
                        new Vector3f(0.0f, 0.0f, 1.0f),
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
        descriptorSets = createDescriptorSets(logicalDevice, descriptorPool, descriptorSetLayout, swapchainImages.size());
        connectDescriptorSetsToUniformBuffers(logicalDevice, descriptorSets, sceneUniformBuffers, sceneData.size(), modelUniformBuffers, modelUniformTransferSpace.elementSize());

        VertexDataStruct.Buffer quad1 = new VertexDataStruct.Buffer(new float[]{
                -0.4f, 0.4f, 0, 1, 0, 0,
                -0.4f, -0.4f, 0, 0, 1, 0,
                0.4f, -0.4f, 0, 0, 0, 1,
                0.4f, 0.4f, 0, 1, 1, 0
        });

        VertexDataStruct.Buffer quad2 = new VertexDataStruct.Buffer(new float[]{
                -0.25f, 0.6f, 0, 1, 0, 0,
                -0.25f, -0.6f, 0, 0, 1, 0,
                0.25f, -0.6f, 0, 0, 0, 1,
                0.25f, 0.6f, 0, 1, 1, 0
        });

        IntBuffer quadIndices = BufferUtils.createIntBuffer(6).put(0, new int[]{0, 1, 2, 2, 3, 0});
        meshData = List.of(quad1, quad2);
        meshes = List.of(
                new Mesh(graphicsQueue, graphicsCommandPool, quad1, quadIndices),
                new Mesh(graphicsQueue, graphicsCommandPool, quad2, quadIndices)
        );

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
    }

    private List<VkDescriptorSet> createDescriptorSets(VkDevice logicalDevice, VkDescriptorPool descriptorPool, VkDescriptorSetLayout descriptorSetLayout, int size) {
        try (VulkanSession vk = new VulkanSession()) {
            LongBuffer setLayouts = vk.stack().mallocLong(size);
            for (int i = 0; i < size; i++) {
                setLayouts.put(i, descriptorSetLayout.address());
            }

            VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .descriptorPool(descriptorPool.address())
                    .pSetLayouts(setLayouts);
            return vk.allocateDescriptorSets(logicalDevice, descriptorSetAllocateInfo);
        }
    }

    private VkDescriptorPool createDescriptorPool(VkDevice logicalDevice, int sceneDescriptorCount, int modelDescriptorCount, int maxSets) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, vk.stack());
            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(sceneDescriptorCount);
            poolSizes.get(1)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                    .descriptorCount(modelDescriptorCount);
            VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .maxSets(maxSets)
                    .pPoolSizes(poolSizes);

            return vk.createDescriptorPool(logicalDevice, descriptorPoolCreateInfo, null);
        }
    }

    private List<VkBuffer> createUniformBuffers(VkDevice logicalDevice, int bufferCount, int bufferSize) {
        List<VkBuffer> buffers = new ArrayList<>(bufferCount);
        for (int i = 0; i < bufferCount; i++) {
            buffers.add(i, createBuffer(logicalDevice, bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));
        }
        return buffers;
    }

    @Override
    public void dispose() {
        throwIfFailed(vkDeviceWaitIdle(logicalDevice));

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
        for (Mesh mesh : meshes) {
            mesh.dispose();
        }
        for (VertexDataStruct.Buffer data : meshData) {
            data.dispose();
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
        }
    }

    public void updateModel(int modelId, Matrix4f transform) {
        meshes.get(modelId).getTransform().set(transform);
    }

    public void updatePushConstant(Matrix4f transform) {
        pushConstant.transform(transform);
    }

    private void updateUniforms(int imageIndex) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDeviceMemory sceneMemory = sceneUniformBuffers.get(imageIndex).memory();
            assert sceneMemory != null;
            long sceneTarget = vk.mapMemoryPointer(logicalDevice, sceneMemory, 0, sceneData.size(), 0);
            memCopy(sceneData.address(), sceneTarget, sceneData.size());
            vk.unmapMemory(logicalDevice, sceneMemory);


            for (int i = 0; i < meshes.size(); i++) {
                modelUniformTransferSpace.get(i).transform(meshes.get(i).getTransform());
            }
            VkDeviceMemory modelMemory = modelUniformBuffers.get(imageIndex).memory();
            assert modelMemory != null;
            long modelBufferSizeBytes = modelUniformTransferSpace.size();
            long modelTarget = vk.mapMemoryPointer(logicalDevice, modelMemory, 0, modelBufferSizeBytes, 0);
            memCopy(modelUniformTransferSpace.address(), modelTarget, modelBufferSizeBytes);
            vk.unmapMemory(logicalDevice, modelMemory);
        }
    }

    private static List<String> getAllGlfwExtensions() {
        PointerBuffer glfwExtensionsBuffer = glfwGetRequiredInstanceExtensions();
        if (glfwExtensionsBuffer == null) {
            throw new GLFWRuntimeException("No set of extensions allowing GLFW integration was found");
        }
        List<String> glfwExtensions = new ArrayList<>(glfwExtensionsBuffer.limit());
        for (int i = 0; i < glfwExtensionsBuffer.capacity(); i++) {
            glfwExtensions.add(i, memASCII(glfwExtensionsBuffer.get(i)));
        }
        return glfwExtensions;
    }

    private static @Nonnull VkPhysicalDevice findFirstSuitablePhysicalDevice(VkInstance instance, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            PointerBuffer physicalDevices = vk.enumeratePhysicalDevices(instance);
            if (physicalDevices.limit() == 0) {
                throw new RuntimeException("Could not find a suitable physical device");
            }

            for (int i = 0; i < physicalDevices.limit(); i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(physicalDevices.get(i), instance);
                if (isSuitableDevice(physicalDevice, surface)) {
                    return physicalDevice;
                }
            }
        }
        throw new RuntimeException("Could not find a suitable physical device");
    }

    private static boolean isSuitableDevice(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            IntBuffer surfaceFormatCountBuffer = vk.stack().mallocInt(1);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatCountBuffer, null);
            int surfaceFormatCount = surfaceFormatCountBuffer.get(0);

            IntBuffer presentationModeCountBuffer = vk.stack().mallocInt(1);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.address(), presentationModeCountBuffer, null);
            int presentationModeCount = presentationModeCountBuffer.get(0);

            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);

            return queueIndices.graphical() >= 0 &&
                   queueIndices.surfaceSupporting() >= 0 &&
                   deviceSupportsExtensions(physicalDevice, List.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)) &&
                   surfaceFormatCount > 0 &&
                   presentationModeCount > 0;
        }
    }

    private static boolean deviceSupportsExtensions(VkPhysicalDevice physicalDevice, List<String> expectedExtensionNames) {
        try (VulkanSession vk = new VulkanSession()) {
            VkExtensionProperties.Buffer availableExtensionsBuffer = vk.enumerateDeviceExtensionProperties(physicalDevice);
            for (String expectedExtensionName : expectedExtensionNames) {
                boolean isSupported = false;
                for (VkExtensionProperties availableExtensionProperty : availableExtensionsBuffer) {
                    if (availableExtensionProperty.extensionNameString().equals(expectedExtensionName)) {
                        isSupported = true;
                    }
                }
                if (!isSupported) {
                    return false;
                }
            }
            return true;
        }
    }

    private static QueueIndices findQueueIndices(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            VkQueueFamilyProperties.Buffer queueFamilies = vk.getPhysicalDeviceQueueFamilyProperties(physicalDevice);
            List<Integer> graphicalQueueIndices = new ArrayList<>(1);
            List<Integer> surfaceSupportingQueueIndices = new ArrayList<>(1);
            for (int i = 0; i < queueFamilies.queueCount(); i++) {
                VkQueueFamilyProperties queueFamily = queueFamilies.get(i);
                if (queueFamily.queueCount() <= 0) {
                    continue;
                }

                if (queueFamily.queueCount() > 0 && (queueFamily.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicalQueueIndices.add(i);
                }

                IntBuffer supportSurfacePointer = vk.stack().mallocInt(1);
                vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface.address(), supportSurfacePointer);

                if (supportSurfacePointer.get(0) != 0) {
                    surfaceSupportingQueueIndices.add(i);
                }
            }
            return new QueueIndices(
                    graphicalQueueIndices.isEmpty() ? -1 : graphicalQueueIndices.getFirst(),
                    surfaceSupportingQueueIndices.isEmpty() ? -1 : surfaceSupportingQueueIndices.getFirst()
            );
        }
    }

    private static VkInstance createInstance(boolean enableDebugging) {
        try (VulkanSession vk = new VulkanSession()) {
            // Info about the app itself
            VkApplicationInfo applicationInfo = VkApplicationInfo.malloc(vk.stack())
                    // We specify the type of struct that this struct is because there is no reflection in C.
                    .sType$Default()
                    .pNext(NULL)
                    .pApplicationName(vk.stack().UTF8("Vulkan Test App"))
                    .applicationVersion(VK_MAKE_API_VERSION(0, 1, 0, 0))
                    .pEngineName(vk.stack().UTF8("Shadow Engine"))
                    .apiVersion(VK_MAKE_API_VERSION(0, 1, 1, 0)); // this affects the app

            // Create flags list
            int flags = 0;

            // Extension list
            HashSet<String> requiredExtensions = new HashSet<>(getAllGlfwExtensions());
            HashSet<String> availableExtensions = vk.enumerateInstanceExtensionProperties().stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toCollection(HashSet::new));
            for (String requiredExtension : requiredExtensions) {
                if (!availableExtensions.contains(requiredExtension)) {
                    throw new RuntimeException("Unsupported extension: " + requiredExtension);
                }
            }
            // Required on macOS in later version of vulkan SDK
            if (availableExtensions.contains(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)) {
                requiredExtensions.add(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME);
                flags |= KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
            }
            if (enableDebugging && availableExtensions.contains(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)) {
                requiredExtensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
            }
            PointerBuffer requiredExtensionsBuffer = vk.stack().pointers(requiredExtensions.stream().map(vk.stack()::UTF8).toArray(ByteBuffer[]::new));

            // Validation layer list
            PointerBuffer requiredLayersBuffer = enableDebugging ? allocateAndValidateLayerList(List.of(
                    // This will not exist if you're using the bundled lwjgl MoltenVK lib.
                    // See README.md#Vulkan_validation_layers
                    "VK_LAYER_KHRONOS_validation"
            ), vk) : null;

            // Create instance
            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(flags)
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledLayerNames(requiredLayersBuffer)
                    .ppEnabledExtensionNames(requiredExtensionsBuffer);

            PointerBuffer instancePointer = vk.stack().mallocPointer(1);
            throwIfFailed(vkCreateInstance(instanceCreateInfo, null, instancePointer));
            return new VkInstance(instancePointer.get(0), instanceCreateInfo);
        }
    }

    private static VkSurfaceKHR createSurface(VkInstance instance, GlfwWindow window) {
        try (VulkanSession vk = new VulkanSession()) {
            LongBuffer surfacePointer = vk.stack().mallocLong(1);
            throwIfFailed(glfwCreateWindowSurface(instance, window.address(), null, surfacePointer));
            return new VkSurfaceKHR(surfacePointer.get(0));
        }
    }

    private static final VkDebugUtilsMessengerCallbackEXT debugCallbackFunction = VkDebugUtilsMessengerCallbackEXT.create(
            (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                String severity = getVkDebugMessageSeverity(messageSeverity);

                String type = getVkDebugMessageType(messageTypes);

                VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                System.err.format(
                        "%s %s: [%s]\n\t%s\n",
                        type, severity, data.pMessageIdNameString(), data.pMessageString()
                );

                /*
                 * false indicates that layer should not bail out of an
                 * API call that had validation failures. This may mean that the
                 * app dies inside the driver due to invalid parameter(s).
                 * That's what would happen without validation layers, so we'll
                 * keep that behavior here.
                 */
                return VK_FALSE;
            }
    );

    private static long createDebugMessenger(VkInstance instance) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .messageSeverity(
                            // VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                            // VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                    )
                    .messageType(
                            VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                    )
                    .pfnUserCallback(debugCallbackFunction)
                    .pUserData(NULL);
            LongBuffer callbackPointer = vk.stack().mallocLong(1);
            vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, callbackPointer);
            return callbackPointer.get(0);
        }
    }

    private static PointerBuffer allocateAndValidateLayerList(List<String> requiredLayerNames, VulkanSession vk) {
        VkLayerProperties.Buffer availableLayersBuffer = vk.enumerateInstanceLayerProperties();
        List<String> availableLayerNames = availableLayersBuffer.stream().map(VkLayerProperties::layerNameString).toList();
        for (String requiredLayerName : requiredLayerNames) {
            boolean found = false;
            for (String availableLayerName : availableLayerNames) {
                if (Objects.equals(availableLayerName, requiredLayerName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Requested layer name is not available: " + requiredLayerName);
            }
        }

        return vk.stack().pointers(requiredLayerNames.stream().map(vk.stack()::UTF8).toArray(ByteBuffer[]::new));
    }

    private static VkDevice createLogicalDevice(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
            // Queues
            List<VkDeviceQueueCreateInfo> queueCreateInfos = new ArrayList<>(1);
            queueCreateInfos.add(VkDeviceQueueCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .queueFamilyIndex(queueIndices.graphical())
                    .pQueuePriorities(vk.stack().floats(1.0f)));
            if (queueIndices.graphical() != queueIndices.surfaceSupporting()) {
                queueCreateInfos.add(VkDeviceQueueCreateInfo.malloc(vk.stack())
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .queueFamilyIndex(queueIndices.surfaceSupporting())
                        .pQueuePriorities(vk.stack().floats(1.0f)));
            }
            VkDeviceQueueCreateInfo.Buffer queueCreateInfosBuffer = VkDeviceQueueCreateInfo.malloc(queueCreateInfos.size());
            for (int i = 0; i < queueCreateInfos.size(); i++) {
                queueCreateInfosBuffer.put(i, queueCreateInfos.get(i));
            }

            // Features
            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(vk.stack());

            // Extensions
            VkExtensionProperties.Buffer availableExtensions = vk.enumerateDeviceExtensionProperties(physicalDevice);
            List<String> availableExtensionNames = availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .toList();
            List<String> requiredExtensionNames = new ArrayList<>();
            requiredExtensionNames.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            for (String availableDeviceExtensionName : availableExtensionNames) {
                if (Objects.equals(availableDeviceExtensionName, "VK_KHR_portability_subset")) {
                    requiredExtensionNames.add("VK_KHR_portability_subset");
                }
            }
            PointerBuffer requiredExtensionNamesBuffer = vk.stack().pointers(requiredExtensionNames.stream().map(vk.stack()::UTF8).toArray(ByteBuffer[]::new));

            // Create
            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queueCreateInfosBuffer) // Also sets queueCreateInfoCount
                    .ppEnabledLayerNames(null)
                    .ppEnabledExtensionNames(requiredExtensionNamesBuffer) // Also sets enabledExtensionCount
                    .pEnabledFeatures(deviceFeatures);

            PointerBuffer logicalDevicePointer = vk.stack().mallocPointer(1);
            throwIfFailed(vkCreateDevice(physicalDevice, deviceCreateInfo, null, logicalDevicePointer));

            return new VkDevice(logicalDevicePointer.get(0), physicalDevice, deviceCreateInfo);
        }
    }

    private static VkQueue findFirstQueueByFamily(VkDevice logicalDevice, int familyIndex) {
        try (VulkanSession vk = new VulkanSession()) {
            PointerBuffer queuePointer = vk.stack().mallocPointer(1);
            vkGetDeviceQueue(logicalDevice, familyIndex, 0, queuePointer);
            return new VkQueue(queuePointer.get(0), logicalDevice);
        }
    }

    private static VkPresentModeKHR findBestPresentationMode(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (VulkanSession vk = new VulkanSession()) {
            List<VkPresentModeKHR> presentationModes = vk.getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface);
            for (VkPresentModeKHR presentationMode : presentationModes) {
                if (presentationMode == VkPresentModeKHR.VK_PRESENT_MODE_MAILBOX_KHR) {
                    return presentationMode;
                }
            }
        }
        // This one is always available (required by vulkan specs)
        return VkPresentModeKHR.VK_PRESENT_MODE_FIFO_KHR;
    }

    private static SwapchainImageConfig findBestSwapchainImageConfig(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, GlfwWindow window) {
        try (VulkanSession vk = new VulkanSession()) {
            VkSurfaceCapabilitiesKHR surfaceCapabilities = vk.getPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface);

            VkSurfaceFormatKHR.Buffer availableSurfaceFormats = vk.getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface);

            VkFormat bestFormat = VkFormat.VK_FORMAT_R8G8B8A8_UNORM;
            VkColorSpaceKHR bestColorSpace = VkColorSpaceKHR.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
            boolean allFormatsSupported = availableSurfaceFormats.limit() == 1 && availableSurfaceFormats.get(0).format() == VK_FORMAT_UNDEFINED;
            if (!allFormatsSupported) {
                for (VkSurfaceFormatKHR availableSurfaceFormat : availableSurfaceFormats) {
                    if ((availableSurfaceFormat.format() == VkFormat.VK_FORMAT_R8G8B8A8_UNORM.getValue() ||
                         availableSurfaceFormat.format() == VkFormat.VK_FORMAT_B8G8R8A8_UNORM.getValue()) &&
                        availableSurfaceFormat.colorSpace() == VkColorSpaceKHR.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR.getValue()) {
                        bestFormat = VkFormat.of(availableSurfaceFormat.format());
                        bestColorSpace = VkColorSpaceKHR.of(availableSurfaceFormat.colorSpace());
                    }
                }
            }

            VkExtent2D bestResolution = surfaceCapabilities.currentExtent();
            if (surfaceCapabilities.currentExtent().width() == Integer.MAX_VALUE) {
                IntBuffer widthPointer = vk.stack().mallocInt(1);
                IntBuffer heightPointer = vk.stack().mallocInt(1);
                glfwGetFramebufferSize(window.address(), widthPointer, heightPointer);
                VkExtent2D min = surfaceCapabilities.minImageExtent();
                VkExtent2D max = surfaceCapabilities.maxImageExtent();
                bestResolution = VkExtent2D.malloc(vk.stack())
                        .width(Math.clamp(widthPointer.get(0), min.width(), max.width()))
                        .height(Math.clamp(heightPointer.get(0), min.height(), max.height()));
            }

            //noinspection resource
            return new SwapchainImageConfig(
                    bestFormat,
                    bestColorSpace,
                    VkExtent2D.malloc().set(bestResolution)
            );
        }
    }

    private static VkSwapchainKHR createSwapchain(VkPhysicalDevice physicalDevice, VkDevice logicalDevice,
                                                  VkSurfaceKHR surface, SwapchainImageConfig swapchainImageConfig,
                                                  VkPresentModeKHR presentationMode) {
        try (VulkanSession vk = new VulkanSession()) {
            VkSurfaceCapabilitiesKHR surfaceCapabilities = vk.getPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface);

            int minImageCount = surfaceCapabilities.minImageCount() + 1; // Have an extra one for triple buffering
            if (surfaceCapabilities.maxImageCount() > 0) { // If the max image count is 0, then that means there is no limit
                minImageCount = Math.min(minImageCount, surfaceCapabilities.maxImageCount());
            }

            VkSharingMode sharingMode;
            IntBuffer queueIndexBuffer;
            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
            if (queueIndices.graphical() == queueIndices.surfaceSupporting()) {
                sharingMode = VkSharingMode.VK_SHARING_MODE_EXCLUSIVE;
                queueIndexBuffer = vk.stack().ints(queueIndices.graphical(), queueIndices.surfaceSupporting());
            } else {
                sharingMode = VkSharingMode.VK_SHARING_MODE_CONCURRENT;
                queueIndexBuffer = vk.stack().ints(queueIndices.graphical());
            }

            VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(vk.stack())
                    .sType$Default()
                    .surface(surface.address())
                    .minImageCount(minImageCount)
                    .imageFormat(swapchainImageConfig.format().getValue())
                    .imageColorSpace(swapchainImageConfig.colorSpace().getValue())
                    .imageExtent(swapchainImageConfig.extent())
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(sharingMode.getValue())
                    .queueFamilyIndexCount(queueIndexBuffer.limit()) // Make a PR to include this into the buffer
                    .pQueueFamilyIndices(queueIndexBuffer)
                    .preTransform(surfaceCapabilities.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentationMode.getValue())
                    .clipped(true)
                    .oldSwapchain(NULL);
            LongBuffer swapchainPointer = vk.stack().mallocLong(1);
            throwIfFailed(vkCreateSwapchainKHR(logicalDevice, swapchainCreateInfo, null, swapchainPointer));
            return new VkSwapchainKHR(swapchainPointer.get(0));
        }
    }

    private static List<SwapchainImage> createSwapchainImageViews(VkDevice logicalDevice, VkSwapchainKHR swapchain, VkFormat format) {
        try (VulkanSession vk = new VulkanSession()) {
            List<VkImage> swapchainImages = vk.getSwapchainImagesKHR(logicalDevice, swapchain);
            return swapchainImages.stream()
                    .map(image -> new SwapchainImage(image, createImageView(logicalDevice, image, format, new VkImageAspectFlags(VK_IMAGE_ASPECT_COLOR_BIT))))
                    .collect(Collectors.toList());
        }
    }

    private static VkImageView createImageView(VkDevice logicalDevice, VkImage image, VkFormat format, VkImageAspectFlags aspectFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .image(image.address())
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format.getValue())
                    .components(VkComponentMapping.malloc(vk.stack())
                            .r(VK_COMPONENT_SWIZZLE_R)
                            .g(VK_COMPONENT_SWIZZLE_G)
                            .b(VK_COMPONENT_SWIZZLE_B)
                            .a(VK_COMPONENT_SWIZZLE_A))
                    .subresourceRange(VkImageSubresourceRange.malloc(vk.stack())
                            .aspectMask(aspectFlags.value())
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(VK_REMAINING_ARRAY_LAYERS));
            LongBuffer imageViewPointer = vk.stack().mallocLong(1);
            throwIfFailed(vkCreateImageView(logicalDevice, imageViewCreateInfo, null, imageViewPointer));
            return new VkImageView(imageViewPointer.get(0));
        }
    }

    private static VkPipelineLayout createPipelineLayout(VkDevice logicalDevice, List<VkDescriptorSetLayout> descriptorSetLayouts) {
        try (VulkanSession vk = new VulkanSession()) {
            LongBuffer descriptorSetLayoutsBuffer = toAddressBuffer(descriptorSetLayouts, vk.stack(), VkDescriptorSetLayout::address);

            VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, vk.stack())
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                    .offset(0)
                    .size(PushConstantStruct.SIZE);

            VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pSetLayouts(descriptorSetLayoutsBuffer)
                    .pPushConstantRanges(pushConstantRange);

            return vk.createPipelineLayout(logicalDevice, pipelineLayoutCreateInfo, null);
        }
    }

    private static VkRenderPass createRenderPass(VkDevice logicalDevice, VkFormat colorFormat, VkFormat depthBufferFormat) {
        try (VulkanSession vk = new VulkanSession()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2, vk.stack());
            // Color Attachment
            // noinspection resource
            attachments.get(0)
                    .format(colorFormat.getValue())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            // Depth Buffer Attachment
            // noinspection resource
            attachments.get(1)
                    .format(depthBufferFormat.getValue())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference.Buffer colorAttachmentRefs = VkAttachmentReference.calloc(1, vk.stack())
            // Color Attachment Reference
                    .attachment(0) // The index in the list that we pass to VkRenderPassCreateInfo.pAttachments
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            // Depth Attachment Reference
            VkAttachmentReference depthAttachmentRef = VkAttachmentReference.calloc(vk.stack())
                    .attachment(1) // The index in the list that we pass to VkRenderPassCreateInfo.pAttachments
                    .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.calloc(1)
                    // Subpass 1
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(colorAttachmentRefs.limit())
                    .pColorAttachments(colorAttachmentRefs)
                    .pDepthStencilAttachment(depthAttachmentRef);

            // We need to determine when layout transitions occur using subpass dependencies
            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.malloc(2, vk.stack());
            // Conversion from VK_IMAGE_LAYOUT_UNDEFINED to VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
            // Start converting after the external pipeline has completely finished and the reading has stopped there
            // End converting before we reach the color attachment output stage, before we read or write anything in that stage.
            dependencies.put(0, VkSubpassDependency.malloc(vk.stack())
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT)

                    .dstSubpass(0) // id of the subpass that we pass into the VkRenderPassCreateInfo.pSubpasses
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

                    .dependencyFlags(0));
            dependencies.put(1, VkSubpassDependency.malloc(vk.stack())
                    .srcSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

                    .dstSubpass(VK_SUBPASS_EXTERNAL) // id of the subpass that we pass into the VkRenderPassCreateInfo.pSubpasses
                    .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)

                    .dependencyFlags(0));

            VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.malloc(vk.stack())
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pAttachments(attachments)
                    .pSubpasses(subpasses)
                    .pDependencies(dependencies);

            return vk.createRenderPass(logicalDevice, renderPassCreateInfo, null);
        }
    }

    private static VkShaderModule createShaderModule(VkDevice logicalDevice, ByteBuffer code) {
        try (VulkanSession vk = new VulkanSession()) {
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pCode(code);

            return vk.createShaderModule(logicalDevice, shaderModuleCreateInfo, null);
        }
    }

    private VkDescriptorSetLayout createDescriptorSetLayout(VkDevice logicalDevice) {
        try (VulkanSession vk = new VulkanSession()) {
            VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.calloc(2, vk.stack());
            descriptorSetLayoutBindings.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                    .pImmutableSamplers(null);
            descriptorSetLayoutBindings.get(1)
                    .binding(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                    .pImmutableSamplers(null);

            VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pBindings(descriptorSetLayoutBindings);

            return vk.createDescriptorSetLayout(logicalDevice, descriptorSetLayoutCreateInfo, null);
        }
    }

    private void connectDescriptorSetsToUniformBuffers(VkDevice logicalDevice, List<VkDescriptorSet> descriptorSets, List<VkBuffer> sceneUniformBuffers, long sceneUniformSize, List<VkBuffer> modelUniformBuffers, long modelUniformElementSize) {
        assert descriptorSets.size() == sceneUniformBuffers.size();
        assert descriptorSets.size() == modelUniformBuffers.size();
        try (VulkanSession vk = new VulkanSession()) {
            for (int i = 0; i < descriptorSets.size(); i++) {
                VkWriteDescriptorSet.Buffer setWrites = VkWriteDescriptorSet.calloc(2, vk.stack());

                VkDescriptorBufferInfo.Buffer sceneDescriptorBufferInfos = VkDescriptorBufferInfo.calloc(1, vk.stack())
                        .buffer(sceneUniformBuffers.get(i).address())
                        .offset(0)
                        .range(sceneUniformSize);
                setWrites.get(0)
                        .sType$Default()
                        .dstSet(descriptorSets.get(i).address())
                        .dstBinding(0)
                        .dstArrayElement(0)
                        .descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                        .pBufferInfo(sceneDescriptorBufferInfos);
                VkDescriptorBufferInfo.Buffer modelDescriptorBufferInfos = VkDescriptorBufferInfo.calloc(1, vk.stack())
                        .buffer(modelUniformBuffers.get(i).address())
                        .offset(0)
                        .range(modelUniformElementSize);
                setWrites.get(1)
                        .sType$Default()
                        .dstSet(descriptorSets.get(i).address())
                        .dstBinding(1)
                        .dstArrayElement(0)
                        .descriptorCount(1)
                        .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                        .pBufferInfo(modelDescriptorBufferInfos);

                vk.updateDescriptorSets(logicalDevice, setWrites, null);
            }
        }
    }

    private static VkPipeline createGraphicsPipeline(VkDevice logicalDevice, VkExtent2D extent, VkPipelineLayout pipelineLayout, VkRenderPass renderPass) {
        try (VulkanSession vk = new VulkanSession()) {
            ByteBuffer vertexShader = readBinaryResource("shaders/vert.spv");
            ByteBuffer fragmentShader = readBinaryResource("shaders/frag.spv");

            VkShaderModule vertexShaderModule = createShaderModule(logicalDevice, vertexShader);
            VkShaderModule fragmentShaderModule = createShaderModule(logicalDevice, fragmentShader);


            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2);
            ByteBuffer main = vk.stack().UTF8("main");
            // Vertex shader stage
            //noinspection resource
            shaderStages.get(0)
                    .sType$Default()
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertexShaderModule.address())
                    .pName(main);
            // Fragment shader stage
            //noinspection resource
            shaderStages.get(1)
                    .sType$Default()
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragmentShaderModule.address())
                    .pName(main);

            ////// VERTEX LAYOUT & SHADER LOCATIONS //////
            VkVertexInputBindingDescription.Buffer vertexInputBindingDescriptions = VkVertexInputBindingDescription.calloc(1, vk.stack())
                    .binding(0)
                    .stride(Vertex.BYTES)
                    .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            VkVertexInputAttributeDescription.Buffer vertexInputAttributeDescriptions = VkVertexInputAttributeDescription.calloc(2, vk.stack());
            vertexInputAttributeDescriptions.get(0)
                    .location(0)
                    .binding(0)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(Vertex.POSITION_OFFSET_BYTES);
            vertexInputAttributeDescriptions.get(1)
                    .location(1)
                    .binding(0)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(Vertex.COLOR_OFFSET_BYTES);
            VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .pVertexBindingDescriptions(vertexInputBindingDescriptions)
                    .pVertexAttributeDescriptions(vertexInputAttributeDescriptions);
            ///////////////////////

            VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            VkViewport.Buffer viewports = VkViewport.calloc(1);
            //noinspection resource
            viewports.get(0)
                    .x(0)
                    .y(0)
                    .width(extent.width())
                    .height(extent.height())
                    .minDepth(0)
                    .maxDepth(1);

            VkRect2D.Buffer scissors = VkRect2D.calloc(1);
            scissors.get(0)
                    .offset(VkOffset2D.malloc(vk.stack()).set(0, 0))
                    .extent(extent);

            VkPipelineViewportStateCreateInfo viewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .viewportCount(viewports.limit())
                    .pViewports(viewports)
                    .scissorCount(scissors.limit())
                    .pScissors(scissors);

            VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK_POLYGON_MODE_FILL)
                    .cullMode(VK_CULL_MODE_NONE) // TODO: Change back to VK_CULL_MODE_BACK_BIT
                    .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                    .depthBiasEnable(false)
                    .lineWidth(1);

            VkPipelineMultisampleStateCreateInfo multisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                    .sampleShadingEnable(false)
                    .minSampleShading(0)
                    .pSampleMask(null)
                    .alphaToCoverageEnable(false)
                    .alphaToOneEnable(false);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachmentState = VkPipelineColorBlendAttachmentState.calloc(1)
                    .blendEnable(true)
                    .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                    .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                    .colorBlendOp(VK_BLEND_OP_ADD)
                    .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                    .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                    .alphaBlendOp(VK_BLEND_OP_ADD)
                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT |
                                    VK_COLOR_COMPONENT_G_BIT |
                                    VK_COLOR_COMPONENT_B_BIT |
                                    VK_COLOR_COMPONENT_A_BIT);

            VkPipelineColorBlendStateCreateInfo colorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .logicOpEnable(false)
                    .attachmentCount(1)
                    .pAttachments(colorBlendAttachmentState);

            VkPipelineDepthStencilStateCreateInfo depthStencilStateCreateInfo = VkPipelineDepthStencilStateCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .depthTestEnable(true)
                    .depthWriteEnable(true)
                    .depthCompareOp(VK_COMPARE_OP_LESS)
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(false);

            VkGraphicsPipelineCreateInfo.Buffer graphicsPipelineCreateInfos = VkGraphicsPipelineCreateInfo.calloc(1);
            //noinspection resource
            graphicsPipelineCreateInfos.get(0)
                    .sType$Default()
                    .stageCount(2)
                    .pStages(shaderStages)
                    .pVertexInputState(vertexInputStateCreateInfo)
                    .pInputAssemblyState(inputAssemblyStateCreateInfo)
                    .pViewportState(viewportStateCreateInfo)
                    .pRasterizationState(rasterizationStateCreateInfo)
                    .pMultisampleState(multisampleStateCreateInfo)
                    .pDepthStencilState(depthStencilStateCreateInfo)
                    .pColorBlendState(colorBlendStateCreateInfo)
                    .pDynamicState(null)
                    .layout(pipelineLayout.address())
                    .renderPass(renderPass.address())
                    .subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);

            List<VkPipeline> pipelines = vk.createGraphicsPipelines(logicalDevice, null, graphicsPipelineCreateInfos, null);

            vk.destroyShaderModule(logicalDevice, fragmentShaderModule, null);
            vk.destroyShaderModule(logicalDevice, vertexShaderModule, null);

            return pipelines.getFirst();
        }
    }

    private static ByteBuffer readBinaryResource(String resourcePath) {
        try (InputStream inputStream = VulkanUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Could not read file");
            }
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            buffer.flip();
            buffer.rewind();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<VkFramebuffer> createFramebuffers(VkDevice logicalDevice, VkRenderPass renderPass, SwapchainImageConfig imageConfig, List<SwapchainImage> swapchainImages, Image depthBufferImage) {
        try (VulkanSession vk = new VulkanSession()) {
            List<VkFramebuffer> framebuffers = new ArrayList<>(swapchainImages.size());
            for (int i = 0; i < swapchainImages.size(); i++) {
                LongBuffer attachments = vk.stack().longs(swapchainImages.get(i).view().address(), depthBufferImage.view().address());
                VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(vk.stack())
                        .sType$Default()
                        .renderPass(renderPass.address())
                        .pAttachments(attachments)
                        .width(imageConfig.extent().width())
                        .height(imageConfig.extent().height())
                        .layers(1);

                LongBuffer framebufferPointer = vk.stack().mallocLong(1);
                throwIfFailed(vkCreateFramebuffer(logicalDevice, framebufferCreateInfo, null, framebufferPointer));

                framebuffers.add(i, new VkFramebuffer(framebufferPointer.get(0)));
            }

            return framebuffers;
        }
    }

    private static VkCommandPool createCommandPool(VkDevice logicalDevice, int queueFamilyIndex) {
        try (VulkanSession vk = new VulkanSession()) {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(queueFamilyIndex);
            LongBuffer commandPoolPointer = vk.stack().mallocLong(1);
            throwIfFailed(vkCreateCommandPool(logicalDevice, commandPoolCreateInfo, null, commandPoolPointer));
            return new VkCommandPool(commandPoolPointer.get(0));
        }
    }

    private static List<VkCommandBuffer> createCommandBuffers(VkDevice logicalDevice, VkCommandPool commandPool, List<VkFramebuffer> framebuffers) {
        try (VulkanSession vk = new VulkanSession()) {
            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .commandPool(commandPool.address())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY) // the secondary level is used to indicate that this buffer can only be run from another buffer (basically you can record a command to run a buffer of commands).
                    .commandBufferCount(framebuffers.size());
            return vk.allocateCommandBuffers(logicalDevice, commandBufferAllocateInfo);
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
            nvkCmdPushConstants(commandBuffer, pipelineLayout.address(), VK_SHADER_STAGE_VERTEX_BIT, 0, PushConstantStruct.SIZE, pushConstant.address());

            for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
                Mesh mesh = meshes.get(meshIndex);

                vkCmdBindVertexBuffers(
                        commandBuffer,
                        0,
                        vk.stack().longs(mesh.getVertexBuffer().address()),
                        vk.stack().longs(0)
                );
                vkCmdBindIndexBuffer(
                        commandBuffer,
                        mesh.getIndexBuffer().address(),
                        0,
                        VK_INDEX_TYPE_UINT32
                );

                int dynamicOffset = meshIndex * modelUniformTransferSpace.elementSize();
                vkCmdBindDescriptorSets(
                        commandBuffer,
                        VK_PIPELINE_BIND_POINT_GRAPHICS,
                        pipelineLayout.address(),
                        0,
                        vk.stack().longs(descriptorSets.get(imageIndex).address()),
                        vk.stack().ints(dynamicOffset)
                );

                vkCmdDrawIndexed(commandBuffer, mesh.getIndexCount(), 1, 0, 0, 0);
            }

            vkCmdEndRenderPass(commandBuffer);
            throwIfFailed(vkEndCommandBuffer(commandBuffer));
        }
    }

    private int findMemoryTypeIndex(VkPhysicalDevice physicalDevice, int allowedTypes, VkMemoryPropertyFlags requiredFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            VkPhysicalDeviceMemoryProperties memoryProperties = vk.getPhysicalDeviceMemoryProperties(physicalDevice);
            for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
                if ((allowedTypes & (1 << i)) != 0 && (memoryProperties.memoryTypes(i).propertyFlags() & requiredFlags.value()) == requiredFlags.value()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private Image createDepthBufferImage(VkDevice logicalDevice, int width, int height) {
        Set<VkFormat> allowedFormats = Set.of(VkFormat.VK_FORMAT_D32_SFLOAT_S8_UINT, VkFormat.VK_FORMAT_D32_SFLOAT, VkFormat.VK_FORMAT_D24_UNORM_S8_UINT, VkFormat.VK_FORMAT_D16_UNORM_S8_UINT);
        VkFormat format = findBestImageFormat(logicalDevice, allowedFormats, VkImageTiling.VK_IMAGE_TILING_OPTIMAL, new VkFormatFeatureFlags(VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT));
        return createImage(logicalDevice, width, height, format,
                VkImageTiling.VK_IMAGE_TILING_OPTIMAL,
                new VkImageUsageFlags(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT),
                new VkMemoryPropertyFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
                new VkImageAspectFlags(VK_IMAGE_ASPECT_DEPTH_BIT));
    }

    private Image createImage(VkDevice logicalDevice, int width, int height, VkFormat format, VkImageTiling tiling, VkImageUsageFlags usageFlags, VkMemoryPropertyFlags memoryFlags, VkImageAspectFlags imageAspectFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc(vk.stack())
                    .sType$Default()
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(format.getValue())
                    .extent(VkExtent3D.calloc(vk.stack())
                            .width(width)
                            .height(height)
                            .depth(1)
                    )
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(tiling.getValue())
                    .usage(usageFlags.value())
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE) // Whether image can be shared between queues
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            VkImage image = vk.createImage(logicalDevice, imageCreateInfo, null);

            VkMemoryRequirements memoryRequirements = vk.getImageMemoryRequirements(logicalDevice, image);
            VkMemoryAllocateInfo memoryAllocateInfo = VkMemoryAllocateInfo.calloc(vk.stack())
                    .sType$Default()
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(findMemoryTypeIndex(logicalDevice.getPhysicalDevice(), memoryRequirements.memoryTypeBits(), memoryFlags));
            VkDeviceMemory deviceMemory = vk.allocateMemory(logicalDevice, memoryAllocateInfo);
            vk.bindImageMemory(logicalDevice, image, deviceMemory, 0);

            VkImageView imageView = createImageView(logicalDevice, image, format, imageAspectFlags);

            return new Image(
                    format,
                    image,
                    deviceMemory,
                    imageView
            );
        }
    }

    private VkFormat findBestImageFormat(VkDevice logicalDevice, Set<VkFormat> possibleFormats, VkImageTiling tiling, VkFormatFeatureFlags featureFlags) {
        try (VulkanSession vk = new VulkanSession()) {
            for (VkFormat format : possibleFormats) {
                VkFormatProperties properties = vk.getPhysicalDeviceFormatProperties(logicalDevice.getPhysicalDevice(), format);

                if (VkImageTiling.VK_IMAGE_TILING_LINEAR.equals(tiling) && (properties.linearTilingFeatures() & featureFlags.value()) == featureFlags.value()) {
                    return format;
                } else if (VkImageTiling.VK_IMAGE_TILING_OPTIMAL.equals(tiling) && (properties.optimalTilingFeatures() & featureFlags.value()) == featureFlags.value()) {
                    return format;
                }
            }
        }

        throw new RuntimeException("Failed to find best image format");
    }
}
