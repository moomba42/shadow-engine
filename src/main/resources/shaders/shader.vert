#version 450

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 color;
layout(location = 2) in vec2 uv;

layout(set = 0, binding = 0) uniform ViewProjection {
    mat4 projection;
    mat4 view;
} viewProjection;

layout(set = 0, binding = 1) uniform Model {
    mat4 model;
} model;

layout(push_constant) uniform PushConstant {
    mat4 model;
} pushConstant;

layout(location = 0) out vec3 out_color;
layout(location = 1) out vec2 out_uv;

void main() {
    out_color = color;
    out_uv = uv;
    gl_Position = viewProjection.projection * viewProjection.view * model.model * pushConstant.model * vec4(position, 1.0);
}


