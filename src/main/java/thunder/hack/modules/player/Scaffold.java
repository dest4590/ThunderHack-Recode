package thunder.hack.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.events.impl.EventSync;
import thunder.hack.modules.Module;
import thunder.hack.modules.client.HudEditor;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.PlaceUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import org.joml.Vector3f;


public class Scaffold extends Module {
    public final Setting<ColorSetting> Color2 = new Setting<>("Color", new ColorSetting(0x8800FF00));
    public Setting<Boolean> rotate = new Setting<>("Rotate", true);
    public Setting<Boolean> allowShift = new Setting<>("AllowShift", false);
    public Setting<Boolean> autoswap = new Setting<>("AutoSwap", true);
    public Setting<Boolean> tower = new Setting<>("Tower", true);
    public Setting<Boolean> safewalk = new Setting<>("SafeWalk", true);
    public Setting<Boolean> echestholding = new Setting<>("EchestHolding", false);
    public Setting<Boolean> render = new Setting<>("Render", true);
    public Setting<Boolean> strictRotate = new Setting<>("StrictRotate", false);

    private final Timer timer = new Timer();
    private BlockPosWithFacing currentblock;
    float[] rotation = new float[2];

    public Scaffold() {
        super("Scaffold", "лучший скафф", Module.Category.PLAYER);
    }

    private BlockPosWithFacing checkNearBlocks(BlockPos blockPos) {
        if (mc.world.getBlockState(blockPos.add(0, -1, 0)).isSolid())
            return new BlockPosWithFacing(blockPos.add(0, -1, 0), Direction.UP);
        else if (mc.world.getBlockState(blockPos.add(-1, 0, 0)).isSolid())
            return new BlockPosWithFacing(blockPos.add(-1, 0, 0), Direction.EAST);
        else if (mc.world.getBlockState(blockPos.add(1, 0, 0)).isSolid())
            return new BlockPosWithFacing(blockPos.add(1, 0, 0), Direction.WEST);
        else if (mc.world.getBlockState(blockPos.add(0, 0, 1)).isSolid())
            return new BlockPosWithFacing(blockPos.add(0, 0, 1), Direction.NORTH);
        else if (mc.world.getBlockState(blockPos.add(0, 0, -1)).isSolid())
            return new BlockPosWithFacing(blockPos.add(0, 0, -1), Direction.SOUTH);
        return null;
    }

    private int findBlockToPlace() {
        if (mc.player.getMainHandStack().getItem() instanceof BlockItem) {
            if (((BlockItem) mc.player.getMainHandStack().getItem()).getBlock().getDefaultState().isSolid())
                return mc.player.getInventory().selectedSlot;
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getCount() != 0) {
                if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                    if (!echestholding.getValue() || (echestholding.getValue() && !mc.player.getInventory().getStack(i).getItem().equals(Item.fromBlock(Blocks.ENDER_CHEST)))) {
                        if (((BlockItem) mc.player.getInventory().getStack(i).getItem()).getBlock().getDefaultState().isSolid())
                            return i;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public void onEnable(){
        rotation = new float[]{mc.player.getYaw(), mc.player.getPitch()};
    }

    private BlockPosWithFacing checkNearBlocksExtended(BlockPos blockPos) {
        BlockPosWithFacing ret = null;

        ret = checkNearBlocks(blockPos);
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(-1, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(1, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, 0, 1));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, 0, -1));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(-2, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(2, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, 0, 2));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, 0, -2));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos.add(0, -1, 0));
        BlockPos blockPos2 = blockPos.add(0, -1, 0);

        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos2.add(1, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos2.add(-1, 0, 0));
        if (ret != null) return ret;

        ret = checkNearBlocks(blockPos2.add(0, 0, 1));
        if (ret != null) return ret;

        return checkNearBlocks(blockPos2.add(0, 0, -1));
    }

    private int countValidBlocks() {
        int n = 36;
        int n2 = 0;

        while (n < 45) {
            if (!mc.player.getInventory().getStack(n >= 36 ? n - 36 : n).isEmpty()) {
                ItemStack itemStack = mc.player.getInventory().getStack(n >= 36 ? n - 36 : n);
                if (itemStack.getItem() instanceof BlockItem) {
                    if (((BlockItem) itemStack.getItem()).getBlock().getDefaultState().isSolid())
                        n2 += itemStack.getCount();
                }
            }
            n++;
        }

        return n2;
    }

    private float[] getRotations(BlockPos blockPos, Direction enumFacing) {
        Vec3d vec3d = new Vec3d((double) blockPos.getX() + 0.5,  blockPos.getY() + 0.99 , (double) blockPos.getZ() + 0.5);
        vec3d = vec3d.add(new Vec3d(new Vector3f(enumFacing.getVector().getX(),enumFacing.getVector().getY(),enumFacing.getVector().getZ())).multiply(0.5));

        Vec3d vec3d2 = PlaceUtility.getEyesPos(mc.player);

        double d = vec3d.x - vec3d2.x;
        double d2 = vec3d.y - vec3d2.y;
        double d3 = vec3d.z - vec3d2.z;
        double d6 = Math.sqrt(d * d + d3 * d3);

        float f = (float) (Math.toDegrees(Math.atan2(d3, d)) - 90.0f);
        float f2 = (float) (-Math.toDegrees(Math.atan2(d2, d6)));

        float[] ret = new float[2];
        ret[0] = mc.player.getYaw() + MathHelper.wrapDegrees(f - mc.player.getYaw());
        ret[1] = mc.player.getPitch() + MathHelper.wrapDegrees(f2 - mc.player.getPitch());

        return ret;
    }

    private void doSafeWalk(EventMove event) {
        double x = event.get_x();
        double y = event.get_y();
        double z = event.get_z();

        if (mc.player.isOnGround() && !mc.player.noClip) {
            double increment;
            for (increment = 0.05D; x != 0.0D && isOffsetBBEmpty(x, 0.0D); ) {
                if (x < increment && x >= -increment) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= increment;
                } else {
                    x += increment;
                }
            }
            while (z != 0.0D && isOffsetBBEmpty(0.0D, z)) {
                if (z < increment && z >= -increment) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= increment;
                } else {
                    z += increment;
                }
            }
            while (x != 0.0D && z != 0.0D && isOffsetBBEmpty(x, z)) {
                if (x < increment && x >= -increment) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= increment;
                } else {
                    x += increment;
                }
                if (z < increment && z >= -increment) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= increment;
                } else {
                    z += increment;
                }
            }
        }
        event.set_x(x);
        event.set_y(y);
        event.set_z(z);
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (fullNullCheck()) return;

        if (safewalk.getValue())
            doSafeWalk(event);
    }

    public void onRender3D(MatrixStack stack) {
        if (render.getValue() && currentblock != null) {
            Render3DEngine.drawFilledBox(stack,new Box(currentblock.position), Render2DEngine.injectAlpha(HudEditor.getColor(0), 150));
            Render3DEngine.drawBoxOutline(new Box(currentblock.position), Render2DEngine.injectAlpha(HudEditor.getColor(0), 230), 2);
        }
    }

    private boolean isOffsetBBEmpty(double x, double z) {
        return !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.1,0,-0.1).offset(x,  -2, z)).iterator().hasNext();
    }

    @EventHandler
    public void onPre(EventSync event) {
        if(strictRotate.getValue()) mc.player.setSprinting(false);
        if(strictRotate.getValue()){
            mc.player.setYaw(rotation[0]);
            mc.player.setPitch(rotation[1]);
        }
        if (countValidBlocks() <= 0) {
            currentblock = null;
            return;
        }
        currentblock = null;

        if (mc.player.isSneaking() && !allowShift.getValue()) return;


        int n2 = findBlockToPlace();
        if (n2 == -1) return;

        Item item = mc.player.getInventory().getStack(n2).getItem();
        if (!(item instanceof BlockItem)) return;
        Block block = ((BlockItem) item).getBlock();

        boolean fullBlock = false;

        BlockPos blockPos2 = new BlockPos((int) Math.floor(mc.player.getX()), (int) (Math.floor(mc.player.getY()) - (fullBlock ? 1.0 : 0.01)), (int) Math.floor(mc.player.getZ()));

        if (!mc.world.getBlockState(blockPos2).isReplaceable()) return;

        currentblock = checkNearBlocksExtended(blockPos2);
        if (currentblock != null) {
            if (rotate.getValue()) {
              //getRotations(currentblock.position, currentblock.facing);

                Vec3d hitVec = new Vec3d(currentblock.position.getX() + 0.5, currentblock.position.getY() + 0.90, currentblock.position.getZ() + 0.5).add(new Vec3d(currentblock.facing.getUnitVector()).multiply(0.5));
                float[] rotations = PlaceUtility.calculateAngle(hitVec);

                if(strictRotate.getValue()){
                    rotation = rotations;
                } else {
                    mc.player.setYaw(rotations[0]);
                    mc.player.setPitch(rotations[1]);
                }
            }
        }
    }

    @EventHandler
    public void onPost(EventPostSync e) {
        if(mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.2,0,-0.2).offset(0,  -0.5, 0)).iterator().hasNext()) return;
        if (currentblock == null) return;
        int prev_item = mc.player.getInventory().selectedSlot;
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            if (autoswap.getValue()) {
                int blockSlot = findBlockToPlace();
                if (blockSlot != -1) {
                    mc.player.getInventory().selectedSlot = blockSlot;
                    mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(blockSlot));
                }
            }
        }
        if ((mc.player.getMainHandStack().getItem() instanceof BlockItem) && ((BlockItem) mc.player.getMainHandStack().getItem()).getBlock().getDefaultState().isSolid()) {
            if (!mc.player.input.jumping || mc.player.input.movementForward != 0.0f || mc.player.input.movementSideways != 0.0f || !tower.getValue()) {
                timer.reset();
            } else {
                mc.player.setVelocity(0.0, 0.42, 0.0);
                if (timer.passedMs(1500)) {
                    mc.player.setVelocity(mc.player.getVelocity().x,-0.28,mc.player.getVelocity().z);
                    timer.reset();
                }
            }
            Vec3d hitVec = new Vec3d(currentblock.position.getX() + 0.5, currentblock.position.getY() + 0.90, currentblock.position.getZ() + 0.5).add(new Vec3d(currentblock.facing.getUnitVector()).multiply(0.5));

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitVec, currentblock.facing, currentblock.position, false));

            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

            if(!strictRotate.getValue()) {
                mc.player.getInventory().selectedSlot = prev_item;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            }
        }
    }


    public record BlockPosWithFacing(BlockPos position, Direction facing){
    }
}
