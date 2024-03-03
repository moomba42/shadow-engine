package com.alexdl.shadowhaven.engine.vulkan;

import com.alexdl.shadowhaven.engine.GLFWRuntimeException;
import com.alexdl.shadowhaven.engine.GlfwWindow;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.enums.VkColorSpaceKHR;
import org.lwjgl.vulkan.enums.VkFormat;
import org.lwjgl.vulkan.enums.VkPresentModeKHR;
import org.lwjgl.vulkan.enums.VkSharingMode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

@SuppressWarnings("SameParameterValue")
public class VulkanUtils {
    public static final String NULL_STRING = null;

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

    private static String getVkDebugMessageType(int messageTypes) {
        String type;
        if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
            type = "GENERAL";
        } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
            type = "VALIDATION";
        } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
            type = "PERFORMANCE";
        } else {
            type = "UNKNOWN";
        }
        return type;
    }

    private static String getVkDebugMessageSeverity(int messageSeverity) {
        String severity;
        if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
            severity = "VERBOSE";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
            severity = "INFO";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            severity = "WARNING";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            severity = "ERROR";
        } else {
            severity = "UNKNOWN";
        }
        return severity;
    }

    public static void throwIfFailed(int vulkanResultCode) {
        if (vulkanResultCode != VK_SUCCESS) {
            throw new VulkanRuntimeException(vulkanResultCode, translateVulkanResult(vulkanResultCode));
        }
    }

    public static String translateVulkanResult(int vulkanResultCode) {
        return switch (vulkanResultCode) {
            // Success codes
            case VK_SUCCESS -> "Command successfully completed.";
            case VK_NOT_READY -> "A fence or query has not yet completed.";
            case VK_TIMEOUT -> "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET -> "An event is signaled.";
            case VK_EVENT_RESET -> "An event is unsignaled.";
            case VK_INCOMPLETE -> "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR ->
                    "A swap chain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

            // Error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED ->
                    "Initialization of an object could not be completed for implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER ->
                    "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
            case VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR ->
                    "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR ->
                    "A surface has changed in such a way that it is no longer compatible with the swap chain, and further presentation requests using the "
                    + "swap chain will fail. Applications must query the new surface properties and recreate their swap chain if they wish to continue"
                    + "presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR ->
                    "The display used by a swap chain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"
                    + " image.";
            case VK_ERROR_VALIDATION_FAILED_EXT -> "A validation layer found an error.";
            default -> "Unknown result code";
        };
    }

    static PointerBuffer enumeratePhysicalDevices(VkInstance instance, MemoryStack stack) {
        IntBuffer physicalDeviceCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, null));

        PointerBuffer physicalDevices = stack.mallocPointer(physicalDeviceCountPointer.get(0));
        throwIfFailed(vkEnumeratePhysicalDevices(instance, physicalDeviceCountPointer, physicalDevices));

        return physicalDevices;
    }

    static VkExtensionProperties.Buffer enumerateDeviceExtensionProperties(VkPhysicalDevice physicalDevice, MemoryStack stack) {
        IntBuffer deviceExtensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateDeviceExtensionProperties(physicalDevice, NULL_STRING, deviceExtensionCountPointer, null));

        VkExtensionProperties.Buffer deviceExtensions = VkExtensionProperties.malloc(deviceExtensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateDeviceExtensionProperties(physicalDevice, NULL_STRING, deviceExtensionCountPointer, deviceExtensions));

        return deviceExtensions;
    }

    static VkExtensionProperties.Buffer enumerateInstanceExtensionProperties(MemoryStack stack) {
        IntBuffer extensionCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(NULL_STRING, extensionCountPointer, null));

        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(extensionCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceExtensionProperties(NULL_STRING, extensionCountPointer, extensions));
        return extensions;
    }

    static VkLayerProperties.Buffer enumerateInstanceLayerProperties(MemoryStack stack) {
        IntBuffer layerCountPointer = stack.mallocInt(1);
        throwIfFailed(vkEnumerateInstanceLayerProperties(layerCountPointer, null));

        VkLayerProperties.Buffer layers = VkLayerProperties.malloc(layerCountPointer.get(0), stack);
        throwIfFailed(vkEnumerateInstanceLayerProperties(layerCountPointer, layers));
        return layers;
    }

    static VkQueueFamilyProperties.Buffer getPhysicalDeviceQueueFamilyProperties(VkPhysicalDevice physicalDevice, MemoryStack stack) {
        IntBuffer queueFamilyCountPointer = stack.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, null);

        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCountPointer.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCountPointer, queueFamilies);
        return queueFamilies;
    }

    static List<VkPresentModeKHR> getPhysicalDeviceSurfacePresentModesKHR(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, MemoryStack stack) {
        IntBuffer presentationModeCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.address(), presentationModeCount, null);
        IntBuffer presentationModeBuffer = stack.mallocInt(presentationModeCount.get(0));
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.address(), presentationModeCount, presentationModeBuffer);

        List<VkPresentModeKHR> list = new ArrayList<>(presentationModeBuffer.limit());
        for (int i = 0; i < presentationModeBuffer.limit(); i++) {
            list.add(i, VkPresentModeKHR.of(presentationModeBuffer.get(i)));
        }
        return list;
    }

    static VkSurfaceFormatKHR.Buffer getPhysicalDeviceSurfaceFormatsKHR(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, MemoryStack stack) {
        IntBuffer surfaceFormatsCount = stack.mallocInt(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatsCount, null);
        VkSurfaceFormatKHR.Buffer surfaceFormatBuffer = VkSurfaceFormatKHR.malloc(surfaceFormatsCount.get(0), stack);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatsCount, surfaceFormatBuffer);
        return surfaceFormatBuffer;
    }

    static VkSurfaceCapabilitiesKHR getPhysicalDeviceSurfaceCapabilitiesKHR(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, MemoryStack stack) {
        VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface.address(), surfaceCapabilities);
        return surfaceCapabilities;
    }

    static List<VkImage> getSwapchainImagesKHR(VkDevice logicalDevice, VkSwapchainKHR swapchain, MemoryStack stack) {
        IntBuffer swapchainImageCount = stack.mallocInt(1);
        vkGetSwapchainImagesKHR(logicalDevice, swapchain.address(), swapchainImageCount, null);
        LongBuffer swapchainImageBuffer = stack.mallocLong(swapchainImageCount.get(0));
        vkGetSwapchainImagesKHR(logicalDevice, swapchain.address(), swapchainImageCount, swapchainImageBuffer);

        List<VkImage> list = new ArrayList<>(swapchainImageBuffer.limit());
        for (int i = 0; i < swapchainImageBuffer.limit(); i++) {
            list.add(i, new VkImage(swapchainImageBuffer.get(i)));
        }
        return list;
    }


    static List<String> getAllGlfwExtensions() {
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

    static VkPhysicalDevice findFirstSuitablePhysicalDevice(VkInstance instance, VkSurfaceKHR surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer physicalDevices = enumeratePhysicalDevices(instance, stack);
            if (physicalDevices.limit() == 0) {
                return null;
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

    static boolean isSuitableDevice(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer surfaceFormatCountBuffer = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.address(), surfaceFormatCountBuffer, null);
            int surfaceFormatCount = surfaceFormatCountBuffer.get(0);

            IntBuffer presentationModeCountBuffer = stack.mallocInt(1);
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

    static boolean deviceSupportsExtensions(VkPhysicalDevice physicalDevice, List<String> expectedExtensionNames) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtensionProperties.Buffer availableExtensionsBuffer = enumerateDeviceExtensionProperties(physicalDevice, stack);
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

    static QueueIndices findQueueIndices(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkQueueFamilyProperties.Buffer queueFamilies = getPhysicalDeviceQueueFamilyProperties(physicalDevice, stack);
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

                IntBuffer supportSurfacePointer = stack.mallocInt(1);
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

    static VkInstance createInstance(String applicationName, boolean enableDebugging) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Info about the app itself
            VkApplicationInfo applicationInfo = VkApplicationInfo.malloc(stack)
                    // We specify the type of struct that this struct is because there is no reflection in C.
                    .sType$Default()
                    .pNext(NULL)
                    .pApplicationName(stack.UTF8(applicationName))
                    .applicationVersion(VK_MAKE_API_VERSION(0, 1, 0, 0))
                    .pEngineName(stack.UTF8("Shadow Engine"))
                    .apiVersion(VK_MAKE_API_VERSION(0, 1, 1, 0)); // this affects the app

            // Create flags list
            int flags = 0;

            // Extension list
            HashSet<String> requiredExtensions = new HashSet<>(getAllGlfwExtensions());
            HashSet<String> availableExtensions = enumerateInstanceExtensionProperties(stack).stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toCollection(HashSet::new));
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
            PointerBuffer requiredExtensionsBuffer = stack.pointers(requiredExtensions.stream().map(stack::UTF8).toArray(ByteBuffer[]::new));

            // Validation layer list
            PointerBuffer requiredLayersBuffer = enableDebugging ? allocateAndValidateLayerList(List.of(
                    // This will not exist if you're using the bundled lwjgl MoltenVK lib.
                    // See README.md#Vulkan_validation_layers
                    "VK_LAYER_KHRONOS_validation"
            ), stack) : null;

            // Create instance
            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(flags)
                    .pApplicationInfo(applicationInfo)
                    .ppEnabledLayerNames(requiredLayersBuffer)
                    .ppEnabledExtensionNames(requiredExtensionsBuffer);

            PointerBuffer instancePointer = stack.mallocPointer(1);
            throwIfFailed(vkCreateInstance(instanceCreateInfo, null, instancePointer));
            return new VkInstance(instancePointer.get(0), instanceCreateInfo);
        }
    }

    public static VkSurfaceKHR createSurface(VkInstance instance, GlfwWindow window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer surfacePointer = stack.mallocLong(1);
            throwIfFailed(glfwCreateWindowSurface(instance, window.address(), null, surfacePointer));
            return new VkSurfaceKHR(surfacePointer.get(0));
        }
    }

    public static long createDebugMessenger(VkInstance instance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.malloc(stack)
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
            LongBuffer callbackPointer = stack.mallocLong(1);
            vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, callbackPointer);
            return callbackPointer.get(0);
        }
    }

    private static PointerBuffer allocateAndValidateLayerList(List<String> requiredLayerNames, MemoryStack stack) {
        VkLayerProperties.Buffer availableLayersBuffer = enumerateInstanceLayerProperties(stack);
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

        return stack.pointers(requiredLayerNames.stream().map(stack::UTF8).toArray(ByteBuffer[]::new));
    }

    public static VkDevice createLogicalDevice(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
            // Queues
            List<VkDeviceQueueCreateInfo> queueCreateInfos = new ArrayList<>(1);
            queueCreateInfos.add(VkDeviceQueueCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .queueFamilyIndex(queueIndices.graphical())
                    .pQueuePriorities(stack.floats(1.0f)));
            if (queueIndices.graphical() != queueIndices.surfaceSupporting()) {
                queueCreateInfos.add(VkDeviceQueueCreateInfo.malloc(stack)
                        .sType$Default()
                        .pNext(NULL)
                        .flags(0)
                        .queueFamilyIndex(queueIndices.surfaceSupporting())
                        .pQueuePriorities(stack.floats(1.0f)));
            }
            VkDeviceQueueCreateInfo.Buffer queueCreateInfosBuffer = VkDeviceQueueCreateInfo.malloc(queueCreateInfos.size(), stack);
            for (int i = 0; i < queueCreateInfos.size(); i++) {
                queueCreateInfosBuffer.put(i, queueCreateInfos.get(i));
            }

            // Features
            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

            // Extensions
            VkExtensionProperties.Buffer availableExtensions = enumerateDeviceExtensionProperties(physicalDevice, stack);
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
            PointerBuffer requiredExtensionNamesBuffer = stack.pointers(requiredExtensionNames.stream().map(stack::UTF8).toArray(ByteBuffer[]::new));

            // Create
            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queueCreateInfosBuffer) // Also sets queueCreateInfoCount
                    .ppEnabledLayerNames(null)
                    .ppEnabledExtensionNames(requiredExtensionNamesBuffer) // Also sets enabledExtensionCount
                    .pEnabledFeatures(deviceFeatures);

            PointerBuffer logicalDevicePointer = stack.mallocPointer(1);
            throwIfFailed(vkCreateDevice(physicalDevice, deviceCreateInfo, null, logicalDevicePointer));

            return new VkDevice(logicalDevicePointer.get(0), physicalDevice, deviceCreateInfo);
        }
    }

    public static VkQueue findFirstQueueByFamily(VkDevice logicalDevice, int familyIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer queuePointer = stack.mallocPointer(1);
            vkGetDeviceQueue(logicalDevice, familyIndex, 0, queuePointer);
            return new VkQueue(queuePointer.get(0), logicalDevice);
        }
    }

    public static VkPresentModeKHR findBestPresentationMode(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            List<VkPresentModeKHR> presentationModes = getPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, stack);
            for (VkPresentModeKHR presentationMode : presentationModes) {
                if (presentationMode == VkPresentModeKHR.VK_PRESENT_MODE_MAILBOX_KHR) {
                    return presentationMode;
                }
            }
        }
        // This one is always available (required by vulkan specs)
        return VkPresentModeKHR.VK_PRESENT_MODE_FIFO_KHR;
    }

    public static SwapchainImageConfig findBestSwapchainImageConfig(VkPhysicalDevice physicalDevice, VkSurfaceKHR surface, GlfwWindow window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR surfaceCapabilities = getPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, stack);

            VkSurfaceFormatKHR.Buffer availableSurfaceFormats = getPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, stack);

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
                IntBuffer widthPointer = stack.mallocInt(1);
                IntBuffer heightPointer = stack.mallocInt(1);
                glfwGetFramebufferSize(window.address(), widthPointer, heightPointer);
                VkExtent2D min = surfaceCapabilities.minImageExtent();
                VkExtent2D max = surfaceCapabilities.maxImageExtent();
                bestResolution = VkExtent2D.malloc(stack)
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

    static VkSwapchainKHR createSwapchain(VkPhysicalDevice physicalDevice, VkDevice logicalDevice,
                                          VkSurfaceKHR surface, SwapchainImageConfig swapchainImageConfig,
                                          VkPresentModeKHR presentationMode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR surfaceCapabilities = getPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, stack);

            int minImageCount = surfaceCapabilities.minImageCount() + 1; // Have an extra one for triple buffering
            if (surfaceCapabilities.maxImageCount() > 0) { // If the max image count is 0, then that means there is no limit
                minImageCount = Math.min(minImageCount, surfaceCapabilities.maxImageCount());
            }

            VkSharingMode sharingMode;
            IntBuffer queueIndexBuffer;
            QueueIndices queueIndices = findQueueIndices(physicalDevice, surface);
            if (queueIndices.graphical() == queueIndices.surfaceSupporting()) {
                sharingMode = VkSharingMode.VK_SHARING_MODE_EXCLUSIVE;
                queueIndexBuffer = stack.ints(queueIndices.graphical(), queueIndices.surfaceSupporting());
            } else {
                sharingMode = VkSharingMode.VK_SHARING_MODE_CONCURRENT;
                queueIndexBuffer = stack.ints(queueIndices.graphical());
            }

            VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
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
            LongBuffer swapchainPointer = stack.mallocLong(1);
            throwIfFailed(vkCreateSwapchainKHR(logicalDevice, swapchainCreateInfo, null, swapchainPointer));
            return new VkSwapchainKHR(swapchainPointer.get(0));
        }
    }

    static List<SwapchainImage> createSwapchainImageViews(VkDevice logicalDevice, VkSwapchainKHR swapchain, VkFormat format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            List<VkImage> swapchainImages = getSwapchainImagesKHR(logicalDevice, swapchain, stack);
            return swapchainImages.stream()
                    .map(image -> new SwapchainImage(image, createImageView(logicalDevice, image, format, new VkImageAspectFlags(VK_IMAGE_ASPECT_COLOR_BIT))))
                    .collect(Collectors.toList());
        }
    }

    static VkImageView createImageView(VkDevice logicalDevice, VkImage image, VkFormat format, VkImageAspectFlags aspectFlags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .image(image.address())
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format.getValue())
                    .components(VkComponentMapping.malloc(stack)
                            .r(VK_COMPONENT_SWIZZLE_R)
                            .g(VK_COMPONENT_SWIZZLE_G)
                            .b(VK_COMPONENT_SWIZZLE_B)
                            .a(VK_COMPONENT_SWIZZLE_A))
                    .subresourceRange(VkImageSubresourceRange.malloc(stack)
                            .aspectMask(aspectFlags.flags())
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(VK_REMAINING_ARRAY_LAYERS));
            LongBuffer imageViewPointer = stack.mallocLong(1);
            throwIfFailed(vkCreateImageView(logicalDevice, imageViewCreateInfo, null, imageViewPointer));
            return new VkImageView(imageViewPointer.get(0));
        }
    }

    static VkPipelineLayout createPipelineLayout(VkDevice logicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .setLayoutCount(0)
                    .pSetLayouts(null)
                    .pPushConstantRanges(null);

            LongBuffer pipelineLayoutPointer = stack.mallocLong(1);
            throwIfFailed(vkCreatePipelineLayout(logicalDevice, pipelineLayoutCreateInfo, null, pipelineLayoutPointer));
            return new VkPipelineLayout(pipelineLayoutPointer.get(0));
        }
    }

    static VkRenderPass createRenderPass(VkDevice logicalDevice, VkFormat format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.malloc(1, stack)
                    // Color Attachment
                    .flags(0)
                    .format(format.getValue())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);


            VkAttachmentReference.Buffer attachmentReferences = VkAttachmentReference.malloc(1, stack)
                    // Color Attachment Reference
                    .attachment(0) // The index in the list that we pass to VkRenderPassCreateInfo.pAttachments
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.malloc(1, stack)
                    // Subpass 1
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(attachmentReferences)
                    .pInputAttachments(null)
                    .pResolveAttachments(null)
                    .pDepthStencilAttachment(null)
                    .pPreserveAttachments(null);

            // We need to determine when layout transitions occur using subpass dependencies
            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.malloc(2, stack);
            // Conversion from VK_IMAGE_LAYOUT_UNDEFINED to VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
            // Start converting after the external pipeline has completely finished and the reading has stopped there
            // End converting before we reach the color attachment output stage, before we read or write anything in that stage.
            dependencies.put(0, VkSubpassDependency.malloc(stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT)

                    .dstSubpass(0) // id of the subpass that we pass into the VkRenderPassCreateInfo.pSubpasses
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

                    .dependencyFlags(0));
            dependencies.put(1, VkSubpassDependency.malloc(stack)
                    .srcSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

                    .dstSubpass(VK_SUBPASS_EXTERNAL) // id of the subpass that we pass into the VkRenderPassCreateInfo.pSubpasses
                    .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                    .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)

                    .dependencyFlags(0));

            VkRenderPassCreateInfo renderPassCreateInfo = VkRenderPassCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pAttachments(attachments)
                    .pSubpasses(subpasses)
                    .pDependencies(dependencies);

            LongBuffer renderPassPointer = stack.mallocLong(1);
            throwIfFailed(vkCreateRenderPass(logicalDevice, renderPassCreateInfo, null, renderPassPointer));
            return new VkRenderPass(renderPassPointer.get(0));
        }
    }

    static VkShaderModule createShaderModule(VkDevice logicalDevice, ByteBuffer code) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pCode(code);

            LongBuffer shaderModulePointer = stack.mallocLong(1);
            throwIfFailed(vkCreateShaderModule(logicalDevice, shaderModuleCreateInfo, null, shaderModulePointer));
            return new VkShaderModule(shaderModulePointer.get(0));
        }
    }

    static VkPipeline createGraphicsPipeline(VkDevice logicalDevice, VkExtent2D extent, VkPipelineLayout pipelineLayout, VkRenderPass renderPass) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer vertexShader = readBinaryResource("shaders/vert.spv");
            ByteBuffer fragmentShader = readBinaryResource("shaders/frag.spv");

            VkShaderModule vertexShaderModule = createShaderModule(logicalDevice, vertexShader);
            VkShaderModule fragmentShaderModule = createShaderModule(logicalDevice, fragmentShader);


            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            ByteBuffer main = stack.UTF8("main");
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

            VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType$Default()
                    .pVertexBindingDescriptions(null)
                    .pVertexAttributeDescriptions(null);

            VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType$Default()
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            VkViewport.Buffer viewports = VkViewport.calloc(1, stack);
            //noinspection resource
            viewports.get(0)
                    .x(0)
                    .y(0)
                    .width(extent.width())
                    .height(extent.height())
                    .minDepth(0)
                    .maxDepth(1);

            VkRect2D.Buffer scissors = VkRect2D.calloc(1, stack);
            scissors.get(0)
                    .offset(VkOffset2D.malloc(stack).set(0, 0))
                    .extent(extent);

            VkPipelineViewportStateCreateInfo viewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType$Default()
                    .viewportCount(viewports.limit())
                    .pViewports(viewports)
                    .scissorCount(scissors.limit())
                    .pScissors(scissors);

            VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType$Default()
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK_POLYGON_MODE_FILL)
                    .cullMode(VK_CULL_MODE_BACK_BIT)
                    .frontFace(VK_FRONT_FACE_CLOCKWISE)
                    .depthBiasEnable(false)
                    .lineWidth(1);

            VkPipelineMultisampleStateCreateInfo multisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType$Default()
                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                    .sampleShadingEnable(false)
                    .minSampleShading(0)
                    .pSampleMask(null)
                    .alphaToCoverageEnable(false)
                    .alphaToOneEnable(false);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachmentState = VkPipelineColorBlendAttachmentState.calloc(1, stack)
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

            VkPipelineColorBlendStateCreateInfo colorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType$Default()
                    .logicOpEnable(false)
                    .attachmentCount(1)
                    .pAttachments(colorBlendAttachmentState);

            VkGraphicsPipelineCreateInfo.Buffer graphicsPipelineCreateInfos = VkGraphicsPipelineCreateInfo.calloc(1, stack);
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
                    .pDepthStencilState(null)
                    .pColorBlendState(colorBlendStateCreateInfo)
                    .pDynamicState(null)
                    .layout(pipelineLayout.address())
                    .renderPass(renderPass.address())
                    .subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);

            LongBuffer graphicsPipelinePointer = stack.mallocLong(1);
            throwIfFailed(vkCreateGraphicsPipelines(logicalDevice, VK_NULL_HANDLE, graphicsPipelineCreateInfos, null, graphicsPipelinePointer));

            vkDestroyShaderModule(logicalDevice, fragmentShaderModule.address(), null);
            vkDestroyShaderModule(logicalDevice, vertexShaderModule.address(), null);

            return new VkPipeline(graphicsPipelinePointer.get(0));
        }
    }

    static ByteBuffer readBinaryResource(String resourcePath) {
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

    static List<VkFramebuffer> createFramebuffers(VkDevice logicalDevice, VkRenderPass renderPass, SwapchainImageConfig imageConfig, List<SwapchainImage> swapchainImages) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            List<VkFramebuffer> framebuffers = new ArrayList<>(swapchainImages.size());
            for (int i = 0; i < swapchainImages.size(); i++) {
                LongBuffer attachments = stack.longs(swapchainImages.get(i).view().address());
                VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(stack)
                        .sType$Default()
                        .renderPass(renderPass.address())
                        .pAttachments(attachments)
                        .width(imageConfig.extent().width())
                        .height(imageConfig.extent().height())
                        .layers(1);

                LongBuffer framebufferPointer = stack.mallocLong(1);
                throwIfFailed(vkCreateFramebuffer(logicalDevice, framebufferCreateInfo, null, framebufferPointer));

                framebuffers.add(i, new VkFramebuffer(framebufferPointer.get(0)));
            }

            return framebuffers;
        }
    }

    static VkCommandPool createCommandPool(VkDevice logicalDevice, int queueFamilyIndex) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType$Default()
                    .queueFamilyIndex(queueFamilyIndex);
            LongBuffer commandPoolPointer = stack.mallocLong(1);
            throwIfFailed(vkCreateCommandPool(logicalDevice, commandPoolCreateInfo, null, commandPoolPointer));
            return new VkCommandPool(commandPoolPointer.get(0));
        }
    }

    static List<VkCommandBuffer> createCommandBuffers(VkDevice logicalDevice, VkCommandPool commandPool, List<VkFramebuffer> framebuffers) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer commandBufferPointers = stack.mallocPointer(framebuffers.size());

            VkCommandBufferAllocateInfo commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType$Default()
                    .commandPool(commandPool.address())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY) // the secondary level is used to indicate that this buffer can only be run from another buffer (basically you can record a command to run a buffer of commands).
                    .commandBufferCount(framebuffers.size());

            throwIfFailed(vkAllocateCommandBuffers(logicalDevice, commandBufferAllocateInfo, commandBufferPointers));

            List<VkCommandBuffer> commandBuffers = new ArrayList<>(commandBufferPointers.limit());
            for (int i = 0; i < commandBufferPointers.limit(); i++) {
                commandBuffers.add(i, new VkCommandBuffer(commandBufferPointers.get(i), logicalDevice));
            }

            return commandBuffers;
        }
    }

    static void recordCommands(VkRenderPass renderPass, VkExtent2D extent, VkPipeline graphicsPipeline, List<VkCommandBuffer> commandBuffers, List<VkFramebuffer> framebuffers) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType$Default()
                    .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);

            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.get(0).color()
                    .float32(0, 0.6f)
                    .float32(1, 0.65f)
                    .float32(2, 0.4f)
                    .float32(3, 1.0f);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType$Default()
                    .renderPass(renderPass.address())
                    .pClearValues(clearValues);
            renderPassBeginInfo.renderArea().extent(extent).offset().set(0, 0);

            for (int i = 0; i < commandBuffers.size(); i++) {
                VkCommandBuffer commandBuffer = commandBuffers.get(i);
                renderPassBeginInfo.framebuffer(framebuffers.get(i).address());

                throwIfFailed(vkBeginCommandBuffer(commandBuffer, commandBufferBeginInfo));
                vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
                vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline.address());
                vkCmdDraw(commandBuffer, 3, 1, 0, 0);
                vkCmdEndRenderPass(commandBuffer);
                throwIfFailed(vkEndCommandBuffer(commandBuffer));
            }
        }
    }
}
