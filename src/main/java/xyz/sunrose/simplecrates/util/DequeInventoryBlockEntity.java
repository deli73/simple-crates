package xyz.sunrose.simplecrates.util;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import xyz.sunrose.simplecrates.CrateBlockEntity;
import xyz.sunrose.simplecrates.SimpleCrates;

import java.util.ArrayDeque;

abstract public class DequeInventoryBlockEntity extends BlockEntity implements Inventory {
    public ArrayDeque<ItemStack> items;

    public DequeInventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract boolean canPush(ItemStack stack);
    public abstract void push(ItemStack stack);
    public abstract ItemStack pop();
    public abstract ItemStack pop(int count);
    public abstract ItemStack peek();
    protected abstract void update();

    public boolean tryPush(ItemStack stack){
        if (canPush(stack)){
            push(stack);
            update();
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items==null || items.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        if(slot==0){
            return peek();
        }
        return null;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = pop(amount);
        update();
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack stack = pop();
        update();
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if(slot==0 && canPush(stack)){
            push(stack);
        }
        update();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }



}
