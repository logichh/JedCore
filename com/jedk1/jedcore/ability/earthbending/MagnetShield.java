package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class MagnetShield extends MetalAbility implements AddonAbility {

	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.RANGE)
	private double range;
	@Attribute(Attribute.COOLDOWN)
	private long cooldownShift;
	@Attribute(Attribute.COOLDOWN)
	private long cooldownClick;
	
	private boolean repelArrows;
	private boolean repelLivingEntities;
	private double velocity;
	private List<Material> materials;
	
	private boolean isShift;

	public MagnetShield(Player player, boolean isShift) {
		super(player);
		
		this.isShift = isShift;
		
		if (!bPlayer.canBend(this) || !bPlayer.canMetalbend()) {
			return;
		}
		
		if (hasAbility(player, MagnetShield.class)) {
			MagnetShield ms = getAbility(player, MagnetShield.class);
			if (ms != null) {
				ms.remove();
			}
		}
		
		setFields();
		
		if (isShift) {
			if (bPlayer.isOnCooldown(this)) {
				return;
			}
			bPlayer.addCooldown(this, cooldownShift);
		} else {
			if (bPlayer.isOnCooldown(this)) {
				return;
			}
			bPlayer.addCooldown(this, cooldownClick);
		}
		
		start();
	}

	private void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		duration = config.getLong("Abilities.Earth.MagnetShield.Duration");
		range = config.getDouble("Abilities.Earth.MagnetShield.Range");
		cooldownShift = config.getLong("Abilities.Earth.MagnetShield.Cooldowns.Shift");
		cooldownClick = config.getLong("Abilities.Earth.MagnetShield.Cooldowns.Click");
		repelArrows = config.getBoolean("Abilities.Earth.MagnetShield.RepelArrows");
		repelLivingEntities = config.getBoolean("Abilities.Earth.MagnetShield.RepelLivingEntities");
		velocity = config.getDouble("Abilities.Earth.MagnetShield.Velocity");
		
		materials = new ArrayList<>();
		for (String materialName : config.getStringList("Abilities.Earth.MagnetShield.Materials")) {
			try {
				materials.add(Material.valueOf(materialName));
			} catch (IllegalArgumentException e) {
				JedCore.log.warning("Invalid material in MagnetShield config: " + materialName);
			}
		}
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline() || player.isDead()) {
			remove();
			return;
		}
		
		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}
		
		if (System.currentTimeMillis() > getStartTime() + duration) {
			remove();
			return;
		}
		
		// Visual effects
		Location loc = player.getLocation().add(0, 1, 0);
		ParticleEffect.CRIT.display(loc, 5, 0.5, 0.5, 0.5, 0.1);
		ParticleEffect.ENCHANTMENT_TABLE.display(loc, 3, 0.3, 0.3, 0.3, 0.05);
		
		// Repel entities
		for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), range, range, range)) {
			if (entity.equals(player)) continue;
			
			boolean shouldRepel = false;
			
			// Check arrows
			if (repelArrows && entity instanceof Arrow) {
				shouldRepel = true;
			}
			
			// Check living entities with metal items
			if (repelLivingEntities && entity instanceof LivingEntity) {
				LivingEntity living = (LivingEntity) entity;
				
				// Check armor
				for (ItemStack armor : living.getEquipment().getArmorContents()) {
					if (armor != null && materials.contains(armor.getType())) {
						shouldRepel = true;
						break;
					}
				}
				
				// Check held items
				if (!shouldRepel) {
					ItemStack mainHand = living.getEquipment().getItemInMainHand();
					ItemStack offHand = living.getEquipment().getItemInOffHand();
					
					if ((mainHand != null && materials.contains(mainHand.getType())) ||
						(offHand != null && materials.contains(offHand.getType()))) {
						shouldRepel = true;
					}
				}
			}
			
			if (shouldRepel) {
				Vector direction = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
				direction.multiply(velocity);
				entity.setVelocity(direction);
				
				// Sound effect
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1.5f);
			}
		}
	}

	@Override
	public void remove() {
		super.remove();
	}

	@Override
	public long getCooldown() {
		return isShift ? cooldownShift : cooldownClick;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public String getName() {
		return "MagnetShield";
	}

	@Override
	public String getDescription() {
		return JedCoreConfig.getConfig(this.player).getString("Abilities.Earth.MagnetShield.Description");
	}

	@Override
	public String getInstructions() {
		return "Hold sneak to activate magnetic shield, or click to toggle it.";
	}

	@Override
	public boolean isHarmlessAbility() {
		return true;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public String getAuthor() {
		return JedCore.dev;
	}

	@Override
	public String getVersion() {
		return JedCore.version;
	}

	@Override
	public boolean isEnabled() {
		return JedCoreConfig.getConfig(this.player).getBoolean("Abilities.Earth.MagnetShield.Enabled");
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}
}
