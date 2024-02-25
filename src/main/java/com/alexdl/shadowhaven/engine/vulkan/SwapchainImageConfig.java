package com.alexdl.shadowhaven.engine.vulkan;

import org.lwjgl.vulkan.enums.VkColorSpaceKHR;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.enums.VkFormat;

public record SwapchainImageConfig(
        VkFormat format,
        VkColorSpaceKHR colorSpace,
        VkExtent2D extent
) {
}
