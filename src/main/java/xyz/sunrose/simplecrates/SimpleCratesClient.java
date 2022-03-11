package xyz.sunrose.simplecrates;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

public class SimpleCratesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(SimpleCrates.CRATE_BE, CrateRenderer::new);
    }
}
