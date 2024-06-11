#version 450

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 uv;
layout(location = 3) in vec3 color;

layout(set = 0, binding = 0) uniform ViewProjection {
    mat4 projection;
    mat4 view;
} viewProjection;

layout(set = 1, binding = 0) uniform Model {
    mat4 model;
    mat4 normal;
} model;

layout(location = 0) out vec3 out_position;
layout(location = 1) out vec3 out_normal;
layout(location = 2) out vec2 out_uv;
layout(location = 3) out vec3 out_color;

void main() {
    vec4 worldPosition = model.model * vec4(position, 1.0);
    vec4 worldNormal = model.normal * vec4(normal, 1.0);

    out_position = worldPosition.xyz;
    out_normal = worldNormal.xyz;
    out_uv = uv;
    out_color = color;

    gl_Position = viewProjection.projection * viewProjection.view * worldPosition;
}


