package foundry.mirror;

import foundry.mirror.client.render.MirrorRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;

public class MirrorModClient {

    public static void initClient() {
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage((stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum) -> {
            if (stage == VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) {
                MirrorRenderer.levelRenderEnd();
            } else if (stage == VeilRenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
                VeilRenderSystem.endLastBatch(bufferSource, MirrorRenderer.MIRROR_RENDER_TYPE.toString());
            }
        });
    }
}