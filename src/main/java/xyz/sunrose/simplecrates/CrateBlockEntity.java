package xyz.sunrose.simplecrates;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import xyz.sunrose.simplecrates.util.ListInventoryBlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CrateBlockEntity extends ListInventoryBlockEntity {
    protected static final int MAX_ITEMS = 64*27*2; //number of items in a double chest full of 64-stacks
    protected static final String INVENTORY_NAME = "Items";

    public Direction FACING;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleCrates.CRATE_BE, pos, state);
        items = new ArrayList<>(1);
        FACING = state.get(CrateBlock.FACING);
    }

    public int getSize() {
        int size = 0;
        if (items.size() > 0) for(ItemStack stack : this.items) {
            size += stack.getCount();
        }
        return size;
    }

    private int getSpace(){
        return MAX_ITEMS - this.getSize();
    }

    protected Item getItem() {
        return items.size() > 0 ? items.get(0).getItem() : Items.AIR;
    }

    @Override
    public boolean canPush(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() <= getSpace()
                && (getItem() == Items.AIR || stack.getItem().equals(getItem()))
                && !stack.isIn(SimpleCrates.BANNED_ITEMS);
    }

    @Override
    public void pushStack(ItemStack stack){
        //combine with exsiting stacks if possible first
        for (ItemStack s : items) {
            if (s != null && ItemStack.canCombine(s, stack) && s.getCount() < s.getMaxCount()) {
                int diff = s.getMaxCount() - s.getCount();
                int available = stack.getCount();
                int toAdd = Math.min(diff, available);
                s.increment(toAdd);
                stack.decrement(toAdd);
                this.markDirty();
            }
        }
        //if there are any items left over, add a new stack
        if(!stack.isEmpty()) {
            items.add(0, stack);
            this.markDirty();
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
            return takeStack(getItem().getMaxCount()); //grab a full stack
        } else if (items.size() < 1) {
            return ItemStack.EMPTY;
        } else {
            return takeStack(items.get(0).getCount()); //otherwise grab the top stack
        }
    }

    @Override
    public ItemStack takeStack(int amount) {
        if (items.size() < 1) {
            return ItemStack.EMPTY;
        }
        ItemStack top = getStack(0);
        ItemStack extracted = top.split(amount); //grab items from the top stack
        if(top.isEmpty()) {
            items.remove(0);
        }
        if(extracted.getCount() < amount && ItemStack.canCombine(extracted, getStack(0))) { //if there's room for more...
            int diff = amount - extracted.getCount();
            items.get(0).decrement(diff); //take the difference from the next stack
            if(items.get(0).getCount() == 0) { //and delete that stack if it's emptied, though that should never happen
                items.remove(0);
            }
            extracted.increment(diff); //then add those items to the new stack
        }

        this.markDirty();
        return extracted;
    }

    @Override
    public void clear() {
        items.clear();
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
        comparatorUpdate();
        PlayerLookup.tracking(this).forEach(player -> player.networkHandler.sendPacket(toUpdatePacket()));
        super.markDirty();
    }
}
