package com.hbm.modules;

import java.util.List;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.BreederRecipes;
import com.hbm.items.ModItems;
import com.hbm.util.ArmorUtil;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.I18nUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;

public class ItemHazardModule {
	
	/**
	 * Dependency injection: It's fun for boys and girls!
	 * All this interface-pattern-wish-wash only exists for three reasons:
	 * -it lets me add item hazards with ease by using self-returning setters
	 * -it's agnositc and also works with ItemBlocks or whatever implementation I want it to work
	 * -it makes the system truly centralized and I don't have to add new cases to 5 different classes when adding a new hazard
	 */

	public float radiation;
	public float digamma;
	public int fire;
	public boolean blinding;
	public boolean asbestos;
	public boolean hydro;
	public float explosive;
	
	public float tempMod = 1F;
	
	public void setMod(float tempMod) {
		this.tempMod = tempMod;
	}
	
	public void addRadiation(float radiation) {
		this.radiation = radiation;
	}
	
	public void addDigamma(float digamma) {
		this.digamma = digamma;
	}
	
	public void addFire(int fire) {
		this.fire = fire;
	}
	
	public void addAsbestos() {
		this.asbestos = true;
	}
	
	public void addBlinding() {
		this.blinding = true;
	}
	
	public void addHydroReactivity() {
		this.hydro = true;
	}
	
	public void addExplosive(float bang) {
		this.explosive = bang;
	}

	public void applyEffects(EntityLivingBase entity, float mod, int slot, boolean currentItem) {
		
		boolean reacher = false;
		
		if(entity instanceof EntityPlayer && !GeneralConfig.enable528)
			reacher = ((EntityPlayer) entity).inventory.hasItem(ModItems.reacher);
			
		if(this.radiation * tempMod > 0) {
			float rad = this.radiation * tempMod * mod / 20F;
			
			if(reacher)
				rad = (float) Math.min(Math.sqrt(rad), rad); //to prevent radiation from going up when being <1
			
			ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, rad);
		}

		if(this.digamma * tempMod > 0)
			ContaminationUtil.applyDigammaData(entity, this.digamma * tempMod * mod / 20F);

		if(this.fire > 0 && !reacher)
			entity.setFire(this.fire);

		if(this.asbestos)
			ContaminationUtil.applyAsbestos(entity, (int) (1 * mod));

		if(this.hydro && currentItem) {

			if(!entity.worldObj.isRemote && entity.isInWater() && entity instanceof EntityPlayer) {
				
				EntityPlayer player = (EntityPlayer) entity;
				ItemStack held = player.getHeldItem();
				
				player.inventory.mainInventory[player.inventory.currentItem] = held.getItem().getContainerItem(held);
				player.inventoryContainer.detectAndSendChanges();
				player.worldObj.newExplosion(null, player.posX, player.posY + player.getEyeHeight() - player.getYOffset(), player.posZ, 2F, true, true);
			}
		}

		if(this.explosive > 0 && currentItem) {

			if(!entity.worldObj.isRemote && entity.isBurning() && entity instanceof EntityPlayer) {
				
				EntityPlayer player = (EntityPlayer) entity;
				ItemStack held = player.getHeldItem();
				
				player.inventory.mainInventory[player.inventory.currentItem] = held.getItem().getContainerItem(held);
				player.inventoryContainer.detectAndSendChanges();
				player.worldObj.newExplosion(null, player.posX, player.posY + player.getEyeHeight() - player.getYOffset(), player.posZ, this.explosive, true, true);
			}
		}

		if(this.blinding && !(entity instanceof EntityPlayer && ArmorUtil.checkForGoggles((EntityPlayer) entity))) {
			((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.blindness.id, 100, 0));
		}
	}
	
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		
		if(this.radiation * tempMod > 0) {
			list.add(EnumChatFormatting.GREEN + "[" + I18nUtil.resolveKey("trait.radioactive") + "]");
			String rad = "" + (Math.floor(radiation * tempMod * 1000) / 1000);
			list.add(EnumChatFormatting.YELLOW + (rad + "RAD/s"));
		}
		
		if(this.fire > 0) {
			list.add(EnumChatFormatting.GOLD + "[" + I18nUtil.resolveKey("trait.hot") + "]");
		}
		
		if(this.blinding) {
			list.add(EnumChatFormatting.DARK_AQUA + "[" + I18nUtil.resolveKey("trait.blinding") + "]");
		}
		
		if(this.asbestos) {
			list.add(EnumChatFormatting.WHITE + "[" + I18nUtil.resolveKey("trait.asbestos") + "]");
		}
		
		if(this.hydro) {
			list.add(EnumChatFormatting.RED + "[" + I18nUtil.resolveKey("trait.hydro") + "]");
		}
		
		if(this.explosive > 0) {
			list.add(EnumChatFormatting.RED + "[" + I18nUtil.resolveKey("trait.explosive") + "]");
		}
		
		if(this.digamma * tempMod > 0) {
			float d = ((int) (digamma * tempMod * 10000F)) / 10F;
			list.add(EnumChatFormatting.RED + "[" + I18nUtil.resolveKey("trait.digamma") + "]");
			list.add(EnumChatFormatting.DARK_RED + "" + d + "mDRX/s");
		}
		
		int[] breeder = BreederRecipes.getFuelValue(stack);
		
		if(breeder != null) {
			list.add(BreederRecipes.getHEATString("[" + I18nUtil.resolveKey("trait.heat", breeder[0]) + "]", breeder[0]));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.breeding", breeder[1]));
			list.add(EnumChatFormatting.YELLOW + I18nUtil.resolveKey("trait.furnace", (breeder[0] * breeder[1] * 5)));
		}
	}

	public boolean onEntityItemUpdate(EntityItem item) {
		
		if(!item.worldObj.isRemote) {
			
			if(this.hydro && item.worldObj.getBlock((int)Math.floor(item.posX), (int)Math.floor(item.posY), (int)Math.floor(item.posZ)).getMaterial() == Material.water) {

				item.setDead();
				item.worldObj.newExplosion(item, item.posX, item.posY, item.posZ, 2F, true, true);
				return true;
			}
			
			if(this.explosive > 0 && item.isBurning()) {

				item.setDead();
				item.worldObj.newExplosion(item, item.posX, item.posY, item.posZ, this.explosive, true, true);
				return true;
			}
		}
		
		return false;
	}
}
