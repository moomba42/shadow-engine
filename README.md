# Shadow Engine
Used in a game im working on. Everything is expected to be run on macOS.

## Setup

### Debugging
To debug, add the program argument `debug` when running. This will make the engine set up the correct validation layers and messengers.

### Vulkan validation layers
LWJGL directly uses MoltenVK without going through the Vulkan loader.
Because of this, the standard validation layers are missing, which means you won't be able to run the engine with debug mode.
To address this:
1. Add the JVM argument `-Dorg.lwjgl.vulkan.libname=<path_to_vulkan_sdk>/macOS/lib/libvulkan.dylib`
    - or set it programmatically with `Configuration.VULKAN_LIBRARY_NAME`
    - This change will make LWJGL utilize the Vulkan loader,
      which is capable of discovering validation loaders and forwarding Vulkan function calls
      to the ICD (MoltenVK, in this case).
2. Set the `VK_LAYER_PATH` & `VK_ICD_FILENAMES` environment variables.
    - `VK_LAYER_PATH` should be `<path_to_vulkan_sdk>/macOS/share/vulkan/explicit_layer.d`
    - `VK_ICD_FILENAMES` should be `<path_to_vulkan_sdk>/macOS/share/vulkan/icd.d/MoltenVK_icd.json`
    - No need to modify `PATH` or `DYLD_LIBRARY_PATH`
3. If you want, you can adjust to which MoltenVK build the Vulkan SDK points to by modifying the `<vulkan_sdk>/macOS/etc/vulkan/icd.d/MoltenVK_icd.json` file.
    - The Vulkan SDK usually lags behind the latest LWJGL snapshot (as MoltenVK is updated frequently)
    - You can point the json file to the LWJGL MoltenVK build instead

### GLFW windows
If using GLFW make sure to set the `-XstartOnFirstThread` JVM argument, otherwise the program will crash instantly.

### Shader compilation
Run the following code:
```shell
cd src/main/resources/shaders
/Users/adlugosz/VulkanSDK/1.3.275.0/macOS/bin/glslangValidator -V shader.vert
/Users/adlugosz/VulkanSDK/1.3.275.0/macOS/bin/glslangValidator -V shader.frag
```

## Leading thoughts
1. Separate data from logic
2. Use interfaces and dependency injection to allow for easier engine modifications

## Required reads
1. LWJGL3 memory management: https://blog.lwjgl.org/memory-management-in-lwjgl-3/
