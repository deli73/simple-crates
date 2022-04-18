package xyz.sunrose.simplecrates.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

abstract public class ListInventoryBlockEntity extends BlockEntity implements Inventory {
    public ArrayList<ItemStack> items;

    public ListInventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract boolean canPush(ItemStack stack);
    public abstract void pushStack(ItemStack stack);
    public abstract ItemStack takeStack();
    public abstract ItemStack takeStack(int amount);

    public boolean tryPush(ItemStack stack){
        if (canPush(stack)){
            pushStack(stack);
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        return items.size() + 1;
    }

    @Override
    public boolean isEmpty() {
        return items==null || items.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        try {
            return items.get(slot);
        } catch (IndexOutOfBoundsException e) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        try {
            return takeStack(amount);
        } catch (IndexOutOfBoundsException e) { // disclaimer we actually have no fucking clue what we're doing
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeStack(int slot) {
        try {
            return takeStack();
        } catch (IndexOutOfBoundsException e) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        try {
            items.set(slot, stack);
        } catch (IndexOutOfBoundsException e) {
            items.add(stack);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot <= size() && canPush(stack);
    }
}
