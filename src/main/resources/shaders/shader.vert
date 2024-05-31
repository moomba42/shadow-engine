#version 450

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 uv;
layout(location = 3) in vec3 color;

layout(set = 0, binding = 0) uniform ViewProjection {
    mat4 projection;
    mat4 view;
} viewProjection;

layout(set = 0, binding = 1) uniform Model {
    mat4 model;
    mat4 normal;
} model;

layout(location = 0) out vec3 out_position;
layout(location = 1) out vec3 out_normal;
layout(location = 2) out vec2 out_uv;
layout(location = 3) out vec3 out_color;

void main() {
    vec4 worldPosition = vec4(position, 1.0) * model.model;
    vec4 worldNormal = vec4(normal, 1.0) * model.normal;

    out_position = worldPosition.xyz;
    out_normal = normalize(worldNormal.xyz);
    out_uv = uv;
    out_color = color;

    gl_Position = viewProjection.projection * viewProjection.view * worldPosition;
}


