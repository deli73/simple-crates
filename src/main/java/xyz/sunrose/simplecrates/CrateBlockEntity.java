package xyz.sunrose.simplecrates;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
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

import javax.annotation.Nullable;
import java.util.ArrayDeque;

public class CrateBlockEntity extends BlockEntity {
    protected static final int MAX_ITEMS = 64*27*2; //number of items in a double chest full of 64-stacks
    protected static final String INVENTORY_NAME = "Items";

    protected Item item = null;
    protected int size = 0;
    protected ArrayDeque<ItemStack> items;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleCrates.CRATE_BE, pos, state);
        items = new ArrayDeque<>(MAX_ITEMS);
    }

    public boolean push(ItemStack stack){
        if(stack.isEmpty()){
            return false;
        }
        if(item == null || stack.getItem().equals(item)){
            ItemStack top = items.peekFirst();
            //combine with top stack if possible first
            if(top != null && ItemStack.canCombine(top, stack) && top.getCount() < top.getMaxCount()){
                item = stack.getItem();
                int diff = top.getMaxCount() - top.getCount();
                int available = stack.getCount();
                int toAdd = Math.min(diff, available);
                top.increment(toAdd);
                stack.decrement(toAdd);
                size += toAdd;
            }
            //if there are any items left over, add a new stack
            if(!stack.isEmpty()) {
                items.push(stack);
                item = stack.getItem();
                size += stack.getCount();
            }
            this.markDirty();
            return true;
        }
        return false;
    }

    public @Nullable ItemStack pop() {
        if(items.isEmpty()){
            return null;
        }
        ItemStack stack = items.pop();
        size -= stack.getCount();
        if(items.isEmpty()){
            item = null;
        }
        this.markDirty();
        return stack;
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
        NbtList list = nbt.getList(INVENTORY_NAME, NbtElement.COMPOUND_TYPE);
        for(NbtElement e : list) {
            ItemStack stack = ItemStack.fromNbt((NbtCompound) e);
            this.push(stack);
        }
    }

    // standard block entity stuff
    private void sync() {
        if(this.world != null && !this.world.isClient) {
            ((ServerWorld) this.world).getChunkManager().markForUpdate(this.pos);
        }
    }

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
}
