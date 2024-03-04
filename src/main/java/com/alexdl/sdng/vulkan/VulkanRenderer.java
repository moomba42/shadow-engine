package com.alexdl.sdng.vulkan;

import com.alexdl.sdng.Disposable;
import com.alexdl.sdng.GlfwWindow;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.enums.VkPresentModeKHR;

import java.util.List;

import static com.alexdl.sdng.vulkan.VulkanUtils.*;
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
    private final VkPipeline graphicsPipeline;

    private final List<VkFramebuffer> framebuffers;
    private final VkCommandPool commandPool;
    private final List<VkCommandBuffer> commandBuffers;

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
        graphicsPipeline = createGraphicsPipeline(logicalDevice, swapchainImageConfig.extent(), pipelineLayout, renderPass);

        framebuffers = createFramebuffers(logicalDevice, renderPass, swapchainImageConfig, swapchainImages);
        commandPool = createCommandPool(logicalDevice, queueIndices.graphical());
        commandBuffers = createCommandBuffers(logicalDevice, commandPool, framebuffers);
        recordCommands(renderPass, swapchainImageConfig.extent(), graphicsPipeline, commandBuffers, framebuffers);
    }

    @Override
    public void dispose() {
        vkDestroyCommandPool(logicalDevice, commandPool.address(), null);
        for (VkFramebuffer framebuffer : framebuffers) {
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
}
