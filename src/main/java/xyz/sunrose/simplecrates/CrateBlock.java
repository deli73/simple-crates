package xyz.sunrose.simplecrates;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class CrateBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.FACING;

    public CrateBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        super.appendProperties(stateManager);
        stateManager.add(FACING);
    }

    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        CrateBlockEntity crate = (CrateBlockEntity) world.getBlockEntity(pos);
        if(crate==null){
            return 0;
        }
        //emulate vanilla container output levels
        int size = crate.getSize();
        float amount = (float)size / CrateBlockEntity.MAX_ITEMS;
        return MathHelper.floor(amount * 14) + (size > 0 ? 1 : 0);
    }

    // USE
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        CrateBlockEntity crateBE = (CrateBlockEntity) world.getBlockEntity(pos);
        ItemStack held = player.getStackInHand(hand);
        Direction side = hit.getSide();
        assert crateBE != null;
        if(side == state.get(FACING)) {
            if (player.isSneaking() && player.getInventory().getEmptySlot() > -1){ //player is sneaking and has inventory space
                ItemStack grabbed = crateBE.takeStack();
                if(grabbed != null){
                    if(held.isEmpty()){
                        player.setStackInHand(hand, grabbed);
                    }
                    else{
                        player.getInventory().offerOrDrop(grabbed);
                    }
                    return ActionResult.SUCCESS;
                }
            }
            else if (crateBE.tryPush(held.copy())) {
                held.decrement(held.getCount()); //item go poof
                return ActionResult.SUCCESS;
            }
            return ActionResult.CONSUME_PARTIAL;
        }
        return ActionResult.PASS;
    }

    private void setSelfStackNBT(CrateBlockEntity crate, ItemStack stack) {
        if(!crate.items.isEmpty()) {
            NbtCompound blockNBT = new NbtCompound();
            crate.writeNbt(blockNBT);
            stack.setSubNbt("BlockEntityTag", blockNBT);
            //set display nbt
            /*NbtCompound displayNBT = new NbtCompound();
            NbtList loreNBT = new NbtList();
            loreNBT.add(NbtString.of(
                    "[{\"text\":\"(Contains Items)\",\"color\":\"purple\"}]"
            ));
            displayNBT.put("Lore", loreNBT);
            stack.setSubNbt("display", displayNBT);*/
        }
    }

    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        super.appendTooltip(stack, world, tooltip, options);
        NbtCompound nbt = BlockItem.getBlockEntityNbt(stack);

        if (nbt != null && nbt.contains(CrateBlockEntity.INVENTORY_NAME)) {
            NbtList list = nbt.getList(CrateBlockEntity.INVENTORY_NAME, NbtElement.COMPOUND_TYPE);
            if (list.size() > 0) {
                int numItems = 0;
                for(NbtElement e : list) {
                    ItemStack listStack = ItemStack.fromNbt((NbtCompound) e);
                    numItems += listStack.getCount(); //calculate the number of items from the NBT
                }
                ItemStack topStack = ItemStack.fromNbt((NbtCompound) list.get(0));
                MutableText itemsText = topStack.getItem().getName().shallowCopy(); //grab the default name of the item
                itemsText.append(" x").append(String.valueOf(numItems)); //add an indicator of how many there are
                tooltip.add(itemsText);
            }
        }
    }

    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        CrateBlockEntity crate = (CrateBlockEntity) world.getBlockEntity(pos);
        if(!world.isClient && crate != null) {
            //make item and set nbt if needed
            ItemStack stack = new ItemStack(SimpleCrates.CRATE_ITEM);
            setSelfStackNBT(crate, stack);
            //drop the item if non-empty or in survival
            if(!crate.items.isEmpty() || !player.isCreative()) {
                ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                entity.setToDefaultPickupDelay();
                world.spawnEntity(entity);
            }
        }

        super.onBreak(world, pos, state, player);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        List<ItemStack> items = super.getDroppedStacks(state, builder);
        for(ItemStack stack : items) {
            if(stack.getItem() == SimpleCrates.CRATE_ITEM) {
                setSelfStackNBT((CrateBlockEntity) builder.get(LootContextParameters.BLOCK_ENTITY), stack);
            }
        }
        return items;
    }

    public BlockState getPlacementState(ItemPlacementContext ctx){
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }

    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }
}
