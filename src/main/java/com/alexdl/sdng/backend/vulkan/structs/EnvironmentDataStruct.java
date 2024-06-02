package com.alexdl.sdng.backend.vulkan.structs;

import static com.alexdl.sdng.backend.vulkan.SizeConstants.VECTOR_3F_SIZE_BYTES;
import static org.lwjgl.system.MemoryUtil.memPutFloat;
import static org.lwjgl.system.MemoryUtil.memPutInt;

public class EnvironmentDataStruct extends MemoryBlock<EnvironmentDataStruct> {
    /**
     * Aligned to 16 because:
     * - We already have an int (size 4), so we cant start at 0
     * - A light struct's biggest component is a vec3
     * - vec3 and vec4 is aligned to 4N (4 * Float.BYTES).
     */
    private static final int LIGHT_SIZE_OFFSET = 16;
    private static final int LIGHT_SIZE_ELEMENT_BYTES = VECTOR_3F_SIZE_BYTES + (Float.BYTES * 4);
    private static final int LIGHT_SIZE_ELEMENT_BYTES_ALIGNED = 32;
    private final int maxLightCount;

    public EnvironmentDataStruct(int maxLightCount) {
        super(LIGHT_SIZE_OFFSET + (maxLightCount * LIGHT_SIZE_ELEMENT_BYTES_ALIGNED));
        this.maxLightCount = maxLightCount;
    }

    public EnvironmentDataStruct(long address, int maxLightCount) {
        super(address, LIGHT_SIZE_OFFSET + (maxLightCount * LIGHT_SIZE_ELEMENT_BYTES_ALIGNED));
        this.maxLightCount = maxLightCount;
    }

    public void setLightCount(int lightCount) {
        assert lightCount >= 0 && lightCount < maxLightCount;
        memPutInt(address(), lightCount);
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    public void setLight(int index, float positionX, float positionY, float positionZ, float outerRadius, float innerRadius, float decaySpeed, float intensity) {
        long address = address() + LIGHT_SIZE_OFFSET + ((long) index * LIGHT_SIZE_ELEMENT_BYTES_ALIGNED);
        memPutFloat(address + (Float.BYTES * 0), positionX);
        memPutFloat(address + (Float.BYTES * 1), positionY);
        memPutFloat(address + (Float.BYTES * 2), positionZ);
        memPutFloat(address + (Float.BYTES * 3), outerRadius);
        memPutFloat(address + (Float.BYTES * 4), innerRadius);
        memPutFloat(address + (Float.BYTES * 5), decaySpeed);
        memPutFloat(address + (Float.BYTES * 6), intensity);
    }

    @Override
    public EnvironmentDataStruct createAt(long address) {
        return new EnvironmentDataStruct(address, maxLightCount);
    }
}
