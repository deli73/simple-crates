package xyz.sunrose.simplecrates;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.*;

//a lot of this is trial and error so it's kind of a mess, sorry
public class CrateRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final TextRenderer textRenderer;
    private static final int WHITE = 16777215;
    private static final float TEXT_OFFSET = 1/64f+0.01f;

    public CrateRenderer(BlockEntityRendererFactory.Context ctx){
        this.textRenderer = ctx.getTextRenderer();
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        CrateBlockEntity crate = (CrateBlockEntity) entity;

        //render item
        if (crate.item != null){
            ItemStack stack = new ItemStack(crate.item);
            int lightFront = WorldRenderer.getLightmapCoordinates(crate.getWorld(), crate.getPos().offset(crate.FACING));


            //im not good at the maths here so im gonna hardcode the translations and rotations, sry </3
            //i could probs make this an enum but cba
            float tx, ty, tz;
            float text_tx, text_ty, text_tz;
            Quaternion rotation;
            tx=0;ty=0;tz=0;
            rotation = Quaternion.IDENTITY;
            switch (crate.FACING) {
                case UP -> {
                    rotation = Vec3f.POSITIVE_X.getDegreesQuaternion(90);
                    //+Z rotates to -Y
                    rotation.hamiltonProduct(Vec3f.NEGATIVE_Y.getDegreesQuaternion(180));
                    tx = 0.5f;
                    ty = 1f;
                    tz = 0.5f;
                    text_tx = tx;
                    text_ty = ty + TEXT_OFFSET;
                    text_tz = tz;
                }
                case DOWN -> {
                    rotation = Vec3f.POSITIVE_X.getDegreesQuaternion(-90);
                    //+Z rotates to +Y
                    rotation.hamiltonProduct(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                    tx = 0.5f;
                    tz = 0.5f;
                    text_tx = tx;
                    text_ty = ty - TEXT_OFFSET;
                    text_tz = tz;
                }
                case EAST -> {
                    rotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(90);
                    tx = 1f;
                    ty = 0.5f;
                    tz = 0.5f;
                    text_tx = tx + TEXT_OFFSET;
                    text_ty = ty;
                    text_tz = tz;
                }
                case WEST -> {
                    rotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(-90);
                    ty = 0.5f;
                    tz = 0.5f;
                    text_tx = tx - TEXT_OFFSET;
                    text_ty = ty;
                    text_tz = tz;
                }
                case NORTH -> {
                    rotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(180);
                    tx = 0.5f;
                    ty = 0.5f;
                    text_tx = tx;
                    text_ty = ty;
                    text_tz = tz - TEXT_OFFSET;
                }
                case SOUTH -> {
                    tx = 0.5f;
                    ty = 0.5f;
                    tz = 1f;
                    text_tx = tx;
                    text_ty = ty;
                    text_tz = tz + TEXT_OFFSET;
                }
                default -> {
                    text_tx = tx;
                    text_ty = ty;
                    text_tz = tz;
                }
            }

            //render item
            matrices.push();
            matrices.translate(tx, ty, tz);
            matrices.multiply(rotation);
            matrices.scale(0.5f,0.5f,0.5f);
            ItemRenderer renderer = MinecraftClient.getInstance().getItemRenderer();
            renderer.renderItem(
                    stack, ModelTransformation.Mode.FIXED, lightFront, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, 0
            );

            matrices.pop();

            //render item count text if there's more than one item in the crate
            if(crate.size > 1){
                String sizeStr;
                if(crate.size < 1000){
                    sizeStr = String.valueOf(crate.size);
                }
                else {
                    float thousands = crate.size/1000f;
                    sizeStr = String.format("%,.1f",thousands).concat("k");
                }
                //sizeStr = String.valueOf(crate.size);
                //trial and error numbers, again
                float stringX = (float)(20.5-this.textRenderer.getWidth(sizeStr));
                float stringY = 12;

                matrices.push();
                matrices.translate(text_tx, text_ty, text_tz);
                matrices.multiply(rotation);
                matrices.scale(1f / 64, 1f / 64, 1f / 64);
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180));

                this.textRenderer.draw(
                        sizeStr, stringX, stringY, WHITE, false, matrices.peek().getPositionMatrix(),
                        vertexConsumers, false, 0, lightFront
                );
                matrices.pop();
            }


        }
    }
}
