package org.lwjgl.vulkan.enums;

import org.lwjgl.vulkan.AMDDisplayNativeHdr;
import org.lwjgl.vulkan.EXTSwapchainColorspace;
import org.lwjgl.vulkan.KHRSurface;

public enum VkColorSpaceKHR {
    VK_COLOR_SPACE_SRGB_NONLINEAR_KHR(KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR),
    VK_COLOR_SPACE_DISPLAY_P3_NONLINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_DISPLAY_P3_NONLINEAR_EXT),
    VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_EXTENDED_SRGB_LINEAR_EXT),
    VK_COLOR_SPACE_DISPLAY_P3_LINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_DISPLAY_P3_LINEAR_EXT),
    VK_COLOR_SPACE_DCI_P3_NONLINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_DCI_P3_NONLINEAR_EXT),
    VK_COLOR_SPACE_BT709_LINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_BT709_LINEAR_EXT),
    VK_COLOR_SPACE_BT709_NONLINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_BT709_NONLINEAR_EXT),
    VK_COLOR_SPACE_BT2020_LINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_BT2020_LINEAR_EXT),
    VK_COLOR_SPACE_HDR10_ST2084_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_HDR10_ST2084_EXT),
    VK_COLOR_SPACE_DOLBYVISION_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_DOLBYVISION_EXT),
    VK_COLOR_SPACE_HDR10_HLG_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_HDR10_HLG_EXT),
    VK_COLOR_SPACE_ADOBERGB_LINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_ADOBERGB_LINEAR_EXT),
    VK_COLOR_SPACE_ADOBERGB_NONLINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_ADOBERGB_NONLINEAR_EXT),
    VK_COLOR_SPACE_PASS_THROUGH_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_PASS_THROUGH_EXT),
    VK_COLOR_SPACE_EXTENDED_SRGB_NONLINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_EXTENDED_SRGB_NONLINEAR_EXT),
    VK_COLOR_SPACE_DISPLAY_NATIVE_AMD(AMDDisplayNativeHdr.VK_COLOR_SPACE_DISPLAY_NATIVE_AMD),
    VK_COLORSPACE_SRGB_NONLINEAR_KHR(KHRSurface.VK_COLORSPACE_SRGB_NONLINEAR_KHR),
    VK_COLOR_SPACE_DCI_P3_LINEAR_EXT(EXTSwapchainColorspace.VK_COLOR_SPACE_DCI_P3_LINEAR_EXT);

    private final int value;

    VkColorSpaceKHR(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static VkColorSpaceKHR of(int value) {
        for (VkColorSpaceKHR mode : values()) {
            if(mode.value == value) {
                return mode;
            }
        }
        throw new IllegalStateException("Unexpected value: " + value);
    }
}
