package xyz.sunrose.simplecrates;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import xyz.sunrose.simplecrates.util.ListInventoryBlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class CrateBlockEntity extends ListInventoryBlockEntity {
    protected static final int MAX_ITEMS = 64*27*2; //number of items in a double chest full of 64-stacks
    protected static final String INVENTORY_NAME = "Items";

    protected Item item = null;
    protected int size = 0;
    public Direction FACING;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleCrates.CRATE_BE, pos, state);
        items = new ArrayList<>(1);
        FACING = state.get(CrateBlock.FACING);
    }

    private int getSpace(){
        return MAX_ITEMS - this.size;
    }

    @Override
    public boolean canPush(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() <= getSpace() && (item == null || stack.getItem().equals(item));
    }

    @Override
    public void pushStack(ItemStack stack){
        //combine with exsiting stacks if possible first
        for (ItemStack s : items) {
            if (s != null && ItemStack.canCombine(s, stack) && s.getCount() < s.getMaxCount()) {
                item = stack.getItem();
                int diff = s.getMaxCount() - s.getCount();
                int available = stack.getCount();
                int toAdd = Math.min(diff, available);
                s.increment(toAdd);
                stack.decrement(toAdd);
                size += toAdd;
                this.comparatorUpdate();
            }
        }
        //if there are any items left over, add a new stack
        if(!stack.isEmpty()) {
            items.add(0, stack);
            item = stack.getItem();
            size += stack.getCount();
            this.comparatorUpdate();
        }


        /* doesn't currently work, but TODO allow for inserting part of a stack when nearly full
        if(this.size > MAX_ITEMS){
            int diff = this.size - MAX_ITEMS;
            stack.increment(diff);
            this.items.peekFirst().decrement(diff);
            size -= diff;
        }*/
    }

    @Override
    public ItemStack takeStack() {
        if (items.size() > 1 && ItemStack.canCombine(getStack(0), getStack(1))) { //if the top two stacks are combinable
            return takeStack(item.getMaxCount()); //grab a full stack
        } else if (items.size() < 1) {
            return null;
        } else {
            return takeStack(items.get(0).getCount()); //otherwise grab the top stack
        }
    }

    @Override
    public ItemStack takeStack(int amount) {
        if (items.size() < 1) {
            return null;
        }
        ItemStack top = items.get(0).copy();
        if(top.getCount() < amount) { //if there's room for more...
            int diff = amount - top.getCount();
            items.get(1).decrement(diff); //take the difference from the next stack
            if(items.get(1).getCount() == 0) { //and delete that stack if it's emptied
                items.remove(1);
            }
            top.increment(diff); //then add those items to the new stack
        }
        items.remove(0); //remove the top stack as we take it

        this.markDirty();
        this.comparatorUpdate();
        return top;
    }

    @Override
    public void clear() {
        items.clear();
        item = null;
        size = 0;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList list = new NbtList();
        for(ItemStack stack : items){
            NbtCompound c = new NbtCompound();
            stack.writeNbt(c);
            list.add(0, c);
        }
        nbt.put(INVENTORY_NAME, list);
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.clear();
        NbtList list = nbt.getList(INVENTORY_NAME, NbtElement.COMPOUND_TYPE);
        for(NbtElement e : list) {
            ItemStack stack = ItemStack.fromNbt((NbtCompound) e);
            this.pushStack(stack);
        }
    }

    private void comparatorUpdate() {
        if(this.world != null && !this.world.isClient) {
            this.world.updateComparators(pos, this.getCachedState().getBlock());
        }
    }

    // standard block entity stuff
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = new NbtCompound();
        writeNbt(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public void markDirty() {
        if(world == null || world.isClient) {return;}
        PlayerLookup.tracking(this).forEach(player -> player.networkHandler.sendPacket(toUpdatePacket()));
        super.markDirty();
    }
}
