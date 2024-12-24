#include veil:fog
#include veil:camera

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec3 Normal;

out vec2 texCoord;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 normal;

void main() {
    vec4 worldPosition = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * worldPosition;

    vertexDistance = fog_distance(ModelViewMat, Position, FogShape);
    vertexColor = Color;
    texCoord0 = UV0;
    normal = NormalMat * Normal;
}
