package com.alexdl.sdng.vulkan;

import org.lwjgl.vulkan.VkImage;
import org.lwjgl.vulkan.VkImageView;

public record SwapchainImage(
        VkImage image,
        VkImageView view
) {
}
