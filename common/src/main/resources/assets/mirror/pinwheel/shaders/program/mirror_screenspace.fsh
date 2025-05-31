#include veil:fog
#include veil:space_helper

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform mat4 ProjMat;
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

const float maxRayDistance = 50.0;

bool rayIsOutofScreen(vec2 ray){
    return (ray.x > 1 || ray.y > 1 || ray.x < 0 || ray.y < 0);
}

vec3 TraceRay(vec3 rayPos, vec3 dir, int iterationCount){
    float sampleDepth;

    for (int i = 0; i < iterationCount; i++){
        rayPos += dir;
        if (rayPos.x > 1.0 || rayPos.y > 1.0 || rayPos.x < 0.0 || rayPos.y < 0.0) {
            break;
        }

        float sampleDepth = texture(DiffuseDepthSampler, rayPos.xy).r;
        float depthDif = rayPos.z - sampleDepth;
        if (depthDif >= 0 && depthDif < 0.003) { //we have a hit
            return texture(DiffuseSampler, rayPos.xy).rgb;
        }
    }

    return FogColor.rgb;
}

void main() {
    //View Space ray calculation
    vec3 pixelPositionTexture;
    pixelPositionTexture.xy = gl_FragCoord.xy / ScreenSize;
    float pixelDepth = texture(DiffuseDepthSampler, pixelPositionTexture.xy).r;
    pixelPositionTexture.z = pixelDepth;
    vec3 positionView = screenToViewSpace(pixelPositionTexture).xyz;
    vec3 reflectionView = normalize(reflect(positionView, normal));

    if (reflectionView.z > 0) {
        discard;
    }

    vec3 rayEndPositionView = positionView + reflectionView * maxRayDistance;

    //Texture Space ray calculation
    vec3 rayEndPositionTexture = viewToScreenSpace(vec4(rayEndPositionView, 1.0));
    vec3 rayDirectionTexture = rayEndPositionTexture.xyz - pixelPositionTexture;

    ivec2 screenSpaceStartPosition = ivec2(pixelPositionTexture.x * ScreenSize.x, pixelPositionTexture.y * ScreenSize.y);
    ivec2 screenSpaceEndPosition = ivec2(rayEndPositionTexture.x * ScreenSize.x, rayEndPositionTexture.y * ScreenSize.y);
    ivec2 screenSpaceDistance = screenSpaceEndPosition - screenSpaceStartPosition;
    int screenSpaceMaxDistance = max(abs(screenSpaceDistance.x), abs(screenSpaceDistance.y)) / 2;
    rayDirectionTexture /= max(screenSpaceMaxDistance, 0.1);

    // #veil:albedo
    vec3 color = TraceRay(pixelPositionTexture, rayDirectionTexture, screenSpaceMaxDistance);
    fragColor = linear_fog(vec4(color, 1.0) * vertexColor, vertexDistance, FogStart, FogEnd, FogColor);
}