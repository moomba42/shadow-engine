package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.Disposable;
import org.lwjgl.vulkan.enums.VkColorSpaceKHR;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.enums.VkFormat;

public record SwapchainImageConfig (
        VkFormat format,
        VkColorSpaceKHR colorSpace,
        VkExtent2D extent
) implements Disposable {
    @Override
    public void dispose() {
        extent.free();
    }
}
