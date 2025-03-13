#include veil:fog

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec2 ScreenSize;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
// #veil:normal
in vec3 normal;

out vec4 fragColor;

void main() {
    vec2 uv = vec2(1.0 - gl_FragCoord.x, gl_FragCoord.y) / ScreenSize.xy;
    // #veil:albedo
    vec4 color = texture(Sampler0, uv) * vertexColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}

