#version 450

layout(location = 0) in vec3 in_position;
layout(location = 1) in vec3 in_normal;
layout(location = 2) in vec2 in_uv;
layout(location = 3) in vec3 in_color;

const int MAX_LIGHTS = 10;
struct Light {
    vec3 position;
    vec3 color;
    float outerRadius;
    float innerRadius;
    float decaySpeed;
};

layout(set = 1, binding = 0) uniform sampler2D diffuseSampler;
layout(set = 2, binding = 0) uniform Environment {
    int lightCount;
    Light lights[MAX_LIGHTS];
} environment;

layout(location = 0) out vec4 out_color;

vec3 calculateLightColor(Light light) {
    vec3 lightVector = light.position - in_position;
    float lightDistance = length(lightVector);

    float strength = max(lightDistance - light.innerRadius, 0.0) / (light.outerRadius - light.innerRadius);

    if (strength >= 1.0) {
        return vec3(0.0);
    }

    vec3 lightDirection = normalize(lightVector);
    float attenuationFactor = pow(1.0 - (strength*strength), 2.0) / (1.0 + (light.decaySpeed * strength));
    float directionFactor = clamp(dot(normalize(in_normal), lightDirection), 0.0, 1.0);
    return directionFactor * attenuationFactor * light.color;// Removing the direction factor makes it easier to see the light's influence.
}

void main() {
    vec3 accumulatedLight = vec3(0.0);
    for (int i = 0; i < environment.lightCount; i++) {
        accumulatedLight += calculateLightColor(environment.lights[i]);
    }
    accumulatedLight = clamp(accumulatedLight, 0.0, 1.0);

    out_color = texture(diffuseSampler, in_uv) * vec4(in_color, 1.0) * vec4(accumulatedLight, 1.0);
    //    out_color = vec4(environment.lights[0].position, 1.0);
}
