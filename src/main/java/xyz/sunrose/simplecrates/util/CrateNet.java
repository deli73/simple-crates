package xyz.sunrose.simplecrates.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.NbtText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import xyz.sunrose.simplecrates.CrateBlockEntity;
import xyz.sunrose.simplecrates.SimpleCrates;

public class CrateNet {
    public static void crateSend(World world, CrateBlockEntity crate){
        if(world != null && !world.isClient){
            String id = crate.getItem().toString();
            BlockPos pos = crate.getPos();
            PacketByteBuf packet = new PacketByteBuf(PacketByteBufs.create()
                    .writeBlockPos(pos)
                    .writeIdentifier(Identifier.tryParse(id))
                    .writeInt(crate.getCount()));
            for (ServerPlayerEntity player : PlayerLookup.tracking((ServerWorld) world, crate.getPos())) {
                ServerPlayNetworking.send(player, SimpleCrates.NET_CRATE_UPDATE, packet);
            }
        }
    }

    public static void crateRecieve(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf packet, PacketSender sender) {
        BlockPos pos = packet.readBlockPos();
        Identifier id = packet.readIdentifier();
        int count = packet.readInt();

        BlockEntity be = client.world.getBlockEntity(pos);
        if(be instanceof CrateBlockEntity crate){
            crate.set(Registry.ITEM.get(id), count);
            crate.markDirty();
        }
    }
}
