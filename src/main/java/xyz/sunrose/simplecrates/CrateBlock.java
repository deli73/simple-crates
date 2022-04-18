package xyz.sunrose.simplecrates;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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

    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        CrateBlockEntity crate = (CrateBlockEntity) world.getBlockEntity(pos);
        if(!world.isClient && crate != null) {
            //make item and set nbt if needed
            ItemStack stack = new ItemStack(SimpleCrates.CRATE_ITEM);
            if(!crate.items.isEmpty()) {
                NbtCompound blockNBT = new NbtCompound();
                crate.writeNbt(blockNBT);
                stack.setSubNbt("BlockEntityTag", blockNBT);
                //set display nbt
                NbtCompound displayNBT = new NbtCompound();
                NbtList loreNBT = new NbtList();
                loreNBT.add(NbtString.of(
                        "[{\"text\":\"(Contains Items)\",\"color\":\"purple\"}]"
                ));
                displayNBT.put("Lore", loreNBT);
                stack.setSubNbt("display", displayNBT);
            }
            //drop the item if non-empty or in survival
            if(!crate.items.isEmpty() || !player.isCreative()) {
                ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                entity.setToDefaultPickupDelay();
                world.spawnEntity(entity);
            }
        }

        super.onBreak(world, pos, state, player);
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
