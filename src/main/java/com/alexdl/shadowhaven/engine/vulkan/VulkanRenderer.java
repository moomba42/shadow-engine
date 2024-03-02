package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.Disposable;
import com.alexdl.shadowhaven.engine.GlfwWindow;
import org.lwjgl.vulkan.VkPipeline;
import org.lwjgl.vulkan.VkPipelineLayout;
import org.lwjgl.vulkan.VkRenderPass;
import org.lwjgl.vulkan.VkSurfaceKHR;
import org.lwjgl.vulkan.VkSwapchainKHR;
import org.lwjgl.vulkan.enums.VkPresentModeKHR;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import java.util.List;

import static com.alexdl.shadowhaven.engine.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer implements Disposable {
    private final GlfwWindow window;

    private final VkInstance instance;
    private final VkSurfaceKHR surface;
    private final Long debugMessengerPointer;

    private final VkPhysicalDevice physicalDevice;
    private final VkDevice logicalDevice;
    private final VkPresentModeKHR presentationMode;
    private final SwapchainImageConfig swapchainImageConfig;
    private final VkSwapchainKHR swapchain;
    private final List<SwapchainImage> swapchainImages;

    private final VkQueue graphicsQueue;
    private final VkQueue surfaceSupportingQueue;
    private final VkPipelineLayout pipelineLayout;
    private final VkRenderPass renderPass;
    private final VkPipeline pipeline;

    public VulkanRenderer(GlfwWindow window, boolean enableDebugging) {
        this.window = window;
        instance = createInstance("Vulkan Test App", enableDebugging);
        surface = createSurface(instance, window);
        debugMessengerPointer = enableDebugging ? VulkanUtils.createDebugMessenger(instance) : null;

        physicalDevice = findFirstSuitablePhysicalDevice(instance, surface);
        logicalDevice = createLogicalDevice(physicalDevice, surface);

        presentationMode = findBestPresentationMode(physicalDevice, surface);
        swapchainImageConfig = findBestSwapchainImageConfig(physicalDevice, surface, window);
        swapchain = createSwapchain(physicalDevice, logicalDevice, surface, swapchainImageConfig, presentationMode);
        swapchainImages = createSwapchainImageViews(logicalDevice, swapchain, swapchainImageConfig.format());

        QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
        graphicsQueue = findFirstQueueByFamily(logicalDevice, queueIndices.graphical());
        surfaceSupportingQueue = findFirstQueueByFamily(logicalDevice, queueIndices.surfaceSupporting());

        pipelineLayout = createPipelineLayout(logicalDevice);
        renderPass = createRenderPass(logicalDevice, swapchainImageConfig.format());
        pipeline = createGraphicsPipeline(logicalDevice, swapchainImageConfig.extent(), pipelineLayout, renderPass);
    }

    @Override
    public void dispose() {
        swapchainImageConfig.dispose();
        vkDestroyPipeline(logicalDevice, pipeline.address(), null);
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
}
