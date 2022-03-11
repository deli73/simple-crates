package xyz.sunrose.simplecrates;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import xyz.sunrose.simplecrates.util.CrateNet;
import xyz.sunrose.simplecrates.util.DequeInventoryBlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayDeque;

public class CrateBlockEntity extends DequeInventoryBlockEntity {
    protected static final int MAX_ITEMS = 64*27*2; //number of items in a double chest full of 64-stacks
    protected static final String INVENTORY_NAME = "Items";
    public ArrayDeque<ItemStack> items;

    protected Item item = null;
    protected int size = 0;
    public Direction FACING;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleCrates.CRATE_BE, pos, state);
        items = new ArrayDeque<>(MAX_ITEMS);
        FACING = state.get(CrateBlock.FACING);
    }

    private int getSpace(){
        return MAX_ITEMS - this.size;
    }

    public Item getItem(){
        if(item == null){
            return Items.AIR;
        }
        return item;
    }

    public int getCount(){
        return size;
    }

    public void set(Item item, int count){
        if (world != null && world.isClient) {
            this.item = item;
            this.size = count;
        }
    }

    @Override
    public boolean canPush(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() <= getSpace() && (item == null || stack.getItem().equals(item));
    }

    @Override
    public void push(ItemStack stack){
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


        /* doesn't currently work, but TODO allow for inserting part of a stack when nearly full
        if(this.size > MAX_ITEMS){
            int diff = this.size - MAX_ITEMS;
            stack.increment(diff);
            this.items.peekFirst().decrement(diff);
            size -= diff;
        }*/

        this.markDirty();
    }

    public ItemStack pop(){
        if(items.isEmpty()){
            return ItemStack.EMPTY;
        }
        return pop(peek().getCount());
    }

    public ItemStack pop(int count) {
        if(items.isEmpty()){
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.pop();
        ItemStack finalStack = stack.split(count);
        size -= finalStack.getCount();
        if(items.isEmpty()){
            item = null;
        }
        if(!stack.isEmpty()){
            items.push(stack);
        }
        this.markDirty();
        return finalStack;
    }

    public ItemStack peek() {
        if(items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return items.peekFirst();
    }

    @Override
    public void clear() {

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

    protected void update(){
        CrateNet.crateSend(world, this);
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
