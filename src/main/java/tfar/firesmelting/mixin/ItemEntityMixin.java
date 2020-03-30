package tfar.firesmelting.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfar.firesmelting.Smelted;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements Smelted {

	@Shadow public abstract ItemStack getStack();

	public void setCooked() {
		this.cooked = true;
	}

	public boolean cooked = false;

	@Inject(method = "damage",at = @At(value = "INVOKE",target = "net/minecraft/entity/ItemEntity.scheduleVelocityUpdate()V"),cancellable = true)
	private void smelt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
		if (cooked && source.isFire()) {
			cir.setReturnValue(false);
			return;
		}
		if (source.isFire()) {
			int size = this.getStack().getCount();//todo, add custom recipe type
			world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new BasicInventory(getStack()), world).ifPresent(smeltingRecipe -> {
				ItemStack result = smeltingRecipe.craft(null);
				ItemEntity cooked = new ItemEntity(world, getX(), getY(), getZ(), result);
				((Smelted) cooked).setCooked();
				if (size > 1) {
					ItemEntity toCook = new ItemEntity(world, getX(), getY(), getZ(),
									new ItemStack(getStack().getItem(), getStack().getCount() - 1));
					world.spawnEntity(toCook);
				}
				world.spawnEntity(cooked);
				this.remove();
				cir.setReturnValue(true);
			});
		}
	}

	public ItemEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}
}
