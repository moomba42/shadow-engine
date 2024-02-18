package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.GLFWRuntimeException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.KHRPortabilityEnumeration;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import javax.annotation.Nullable;
import java.nio.IntBuffer;
import java.util.Objects;

import static com.alexdl.shadowhaven.engine.vulkan.VulkanUtils.NULL_STRING;
import static com.alexdl.shadowhaven.engine.vulkan.VulkanUtils.throwIfFailed;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer {
    private static final int VULKAN_SDK_VERSION = VK_MAKE_API_VERSION(0, 1, 3, 215);
    private static final int PORTABILITY_REQUIREMENT_SDK_VERSION = VK_MAKE_API_VERSION(0, 1, 3, 216);
    private static final boolean REQUIRES_PORTABILITY_EXTENSION = Platform.get().equals(Platform.MACOSX) && VULKAN_SDK_VERSION >= PORTABILITY_REQUIREMENT_SDK_VERSION;
    private final long glfwWindowPointer;
    private final VkInstance instance;

    public VulkanRenderer(long glfwWindowPointer) {
        this.glfwWindowPointer = glfwWindowPointer;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Info about the app itself
            VkApplicationInfo applicationInfo = VkApplicationInfo.malloc(stack)
                    // We specify the type of struct that this struct is because there is no reflection in C.
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8("Vulkan Test App"))
                    .applicationVersion(VK_MAKE_API_VERSION(0, 1, 0, 0))
                    .pEngineName(stack.UTF8("Shadow Engine"))
                    .apiVersion(VK_MAKE_API_VERSION(0, 1, 0, 0)); // this affects the app

            // Create required extensions list
            PointerBuffer requiredExtensionNamesAscii = stack.mallocPointer(64);
            addGlfwExtensions(requiredExtensionNamesAscii);
            if(REQUIRES_PORTABILITY_EXTENSION) {
                // This is needed for vulkan sdk version >= 1.3.216 on macOS
                requiredExtensionNamesAscii.put(memASCII(KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME));
            }
            requiredExtensionNamesAscii.flip();

            // Get actual extension list and compare with required
            VkExtensionProperties.Buffer actualExtensions = simpleVkEnumerateInstanceExtensionProperties(NULL_STRING, stack);
            String unsupportedExtension = findFirstUnsupportedExtension(actualExtensions, requiredExtensionNamesAscii);
            if(unsupportedExtension != null) {
                throw new RuntimeException("Unsupported extension: "+unsupportedExtension);
            }

            // Create flags list
            int flags = 0;
            if(REQUIRES_PORTABILITY_EXTENSION) {
                // This is needed for vulkan sdk version >= 1.3.216 on macOS
                flags |= KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
            }

            // Info to create a Vulkan instance
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .flags(flags)
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledLayerNames(null) // null is interpreted as null pointer
                    .ppEnabledExtensionNames(requiredExtensionNamesAscii);

            PointerBuffer instancePointer = stack.mallocPointer(1);
            throwIfFailed(vkCreateInstance(createInfo, null, instancePointer));
            instance = new VkInstance(instancePointer.get(0), createInfo);
        }
    }

    private void addGlfwExtensions(PointerBuffer target) {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
        if(glfwExtensions == null) {
            throw new GLFWRuntimeException("No set of extensions allowing GLFW integration was found");
        }
        for (int i = 0; i < glfwExtensions.capacity(); i++) {
            target.put(glfwExtensions.get(i));
        }
    }

    private String findFirstUnsupportedExtension(VkExtensionProperties.Buffer actualExtensions, PointerBuffer requiredExtensionNamesAscii) {
        for (int i = 0; i < requiredExtensionNamesAscii.limit(); i++) {
            String requiredExtensionName = requiredExtensionNamesAscii.getStringASCII(i);
            boolean isContained = false;
            for (int j = 0; j < actualExtensions.capacity(); j++) {
                String actualExtensionName = actualExtensions.get(j).extensionNameString();
                if(Objects.equals(requiredExtensionName, actualExtensionName)) {
                    isContained = true;
                }
            }
            if(!isContained) {
                return requiredExtensionName;
            }
        }
        return null;
    }

    private VkExtensionProperties.Buffer simpleVkEnumerateInstanceExtensionProperties(@Nullable String layerName, MemoryStack stack) {
        IntBuffer propertyCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(layerName, propertyCountPointer, null));
        int propertyCount = propertyCountPointer.get(0);

        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(propertyCount, stack);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(layerName, propertyCountPointer, extensions));
        return extensions;
    }


}
