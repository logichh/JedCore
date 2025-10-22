package com.jedk1.jedcore.ability.waterbending;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.waterbending.ice.PhaseChange;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.jedk1.jedcore.JedCore;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.IceAbility;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

public class FrostBreath extends IceAbility implements AddonAbility {

	private static final List<Material> INVALID_MATERIALS = new ArrayList<Material>(){{
		addAll(Arrays.asList(
				Material.ICE,
				Material.LAVA,
				Material.AIR,
				Material.VOID_AIR,
				Material.CAVE_AIR));
		if (GeneralMethods.getMCVersion() >= 1170) {
			add(Material.getMaterial("LIGHT"));
		}
	}};

	//Savannas are 1.0 temp with 0 humidity. Deserts are 2.0 temp with 0 humidity.
	private static float MAX_TEMP = 1.0F;
	private static float MIN_HUMIDITY = 0.01F;

	public Config config;

	private State state;
	private final List<FrozenBlock> frozenBlocks = new ArrayList<>();

	public FrostBreath(Player player) {
		super(player);

		if (!bPlayer.canBend(this) || !bPlayer.canIcebend()) {
			return;
		}

		this.config = new Config(player);
		this.state = new BeamState();

		double temp = player.getLocation().getWorld().getTemperature(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
		double humidity = player.getLocation().getWorld().getHumidity(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());

		if (config.restrictBiomes && (temp >= MAX_TEMP || humidity <= MIN_HUMIDITY)) {
			return;
		}

		start();
	}

	@Override
	public void progress() {
		if (!state.update()) {
			remove();
		}

		long time = System.currentTimeMillis();

		frozenBlocks.removeIf(frozen -> {
			if (time >= frozen.endTime) {
				removeFrozenBlock(frozen.tempBlock);
				frozen.tempBlock.revertBlock();
				return true;
			}

			return false;
		});
	}

	@Override
	public void remove() {
		super.remove();

		frozenBlocks.forEach(fb -> {
			removeFrozenBlock(fb.tempBlock);
			fb.tempBlock.revertBlock();
		});
		frozenBlocks.clear();
	}

	@Override
	public long getCooldown() {
		return config.cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "FrostBreath";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
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
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return "* JedCore Addon *\n" + config.getString("Abilities.Water.FrostBreath.Description");
	}

	public static List<Material> getInvalidMaterials() {
		return INVALID_MATERIALS;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public List<FrozenBlock> getFrozenBlocks() {
		return frozenBlocks;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.FrostBreath.Enabled");
	}

	private void addFrozenBlock(TempBlock tempBlock) {
		PhaseChange.getFrozenBlocksMap().put(tempBlock, player);
	}

	private void removeFrozenBlock(TempBlock tempBlock) {
		PhaseChange.getFrozenBlocksMap().remove(tempBlock);
	}

	private interface State {
		boolean update();
	}

	private class BeamState implements State {
		@Override
		public boolean update() {
			if (player == null || !player.isOnline()) {
				return transition();
			}

			if (!bPlayer.canBendIgnoreCooldowns(FrostBreath.this)) {
				return transition();
			}

			if (!player.isSneaking() || player.isDead()) {
				return transition();
			}

			if (System.currentTimeMillis() >= getStartTime() + config.duration) {
				return transition();
			}

			createBeam();

			return true;
		}

		private boolean transition() {
			state = new SnowMeltingState();

			return true;
		}

		private boolean isLocationSafe(Location loc) {
			Block block = loc.getBlock();

			if (RegionProtection.isRegionProtected(player, loc, FrostBreath.this)) {
				return false;
			}

			return isTransparent(block);
		}

		private boolean isFreezable(Location location, Entity entity) {
			if (RegionProtection.isRegionProtected(FrostBreath.this, location)) {
				return false;
			}

			if (entity instanceof Player && Commands.invincible.contains(entity.getName())) {
				return false;
			}

			return !location.getBlock().getType().isSolid();
		}

		private void createBeam() {
			Location loc = player.getEyeLocation();
			Vector dir = player.getLocation().getDirection();
			double step = 1;
			double size = 0;
			double offset = 0;
			double damageRegion = 1.5;

			for (double i = 0; i < config.range; i += step) {
				loc = loc.add(dir.clone().multiply(step));
				size += 0.005;
				offset += 0.3;
				damageRegion += 0.01;

				if (!isLocationSafe(loc))
					return;

				for (Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, damageRegion)) {
					if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId()) {
						for (Location cageLocation : createCage(entity.getLocation())) {
							if (isFreezable(cageLocation, entity)) {
								Block block = cageLocation.getBlock();

								updateFrozenBlock(block, Material.ICE, config.freezeDuration);
							}
						}

						if (config.slowEnabled) {
							((LivingEntity) entity).addPotionEffect(JedCore.plugin.getPotionEffectAdapter().getSlownessEffect((int) config.slowDuration, 5));
						}

						if (config.damageEnabled) {
							if (entity instanceof Player) {
								DamageHandler.damageEntity(entity, config.playerDamage, FrostBreath.this);
							} else {
								DamageHandler.damageEntity(entity, config.mobDamage, FrostBreath.this);
							}
						}
					}
				}

				if (config.snowEnabled) {
					freezeGround(loc);
				}

				ParticleEffect.SNOW_SHOVEL.display(loc, config.particles, Math.random(), Math.random(), Math.random(), size);

				JCMethods.displayColoredParticles("#DCDCDC", loc, 1, Math.random(), Math.random(), Math.random(), 0.003f);
				JCMethods.displayColoredParticles("#9696FF", loc, 1, Math.random(), Math.random(), Math.random(), 0.0035f);
			}
		}

		private Location getOffsetLocation(Location loc, double offset) {
			return loc.clone().add((float) ((Math.random() - 0.5) * offset), (float) ((Math.random() - 0.5) * offset), (float) ((Math.random() - 0.5) * offset));
		}

		private void freezeGround(Location loc) {
			for (Location l : GeneralMethods.getCircle(loc, 2, 2, false, true, 0)) {
				if (!RegionProtection.isRegionProtected(player, l, FrostBreath.this)) {
					Block block = l.getBlock();

					if (isWater(l.getBlock())) {
						updateFrozenBlock(block, Material.ICE, config.frozenWaterDuration);
					} else if (isTransparent(l.getBlock()) && l.clone().add(0, -1, 0).getBlock().getType().isSolid() && !INVALID_MATERIALS.contains(l.clone().add(0, -1, 0).getBlock().getType())) {
						if (config.bendSnow) {
							updateFrozenBlock(block, Material.SNOW, config.snowDuration);
						} else {
							TempBlock current = TempBlock.get(block);

							// Refresh any existing TempBlock so the timer resets.
							if (current != null) {
								current.revertBlock();
							}

							TempBlock tempBlock = new TempBlock(block, Material.SNOW.createBlockData());
							tempBlock.setRevertTime(config.snowDuration);
						}
					}
				}
			}
		}

		private void updateFrozenBlock(Block block, Material type, long duration) {
			// Store the TempBlock as a FrozenBlock block so it can be reverted later.
			for (FrozenBlock fb : frozenBlocks) {
				if (fb.tempBlock.getBlock().equals(block)) {
					if (fb.tempBlock.getBlockData().getMaterial() != type) {
						// Completely overwrite this FrozenBlock if the new type doesn't match the old one.
						removeFrozenBlock(fb.tempBlock);
						fb.tempBlock.revertBlock();
						frozenBlocks.remove(fb);
						break;
					}

					fb.endTime = System.currentTimeMillis() + duration;
					return;
				}
			}

			TempBlock tempBlock = new TempBlock(block, type.createBlockData());

			frozenBlocks.add(new FrozenBlock(tempBlock, System.currentTimeMillis() + duration));

			// Add the TempBlock to a ProjectKorra block list so it can be used as a water source.
			// I don't believe there exists a way to make a TempBlock water bendable right now, so this
			// is a hack to make it work.
			addFrozenBlock(tempBlock);
		}

		private List<Location> createCage(Location centerBlock) {
			List<Location> selectedBlocks = new ArrayList<>();

			int bX = centerBlock.getBlockX();
			int bY = centerBlock.getBlockY();
			int bZ = centerBlock.getBlockZ();

			for (int x = bX - 1; x <= bX + 1; x++) {
				for (int y = bY - 1; y <= bY + 1; y++) {
					Location l = new Location(centerBlock.getWorld(), x, y, bZ);
					selectedBlocks.add(l);
				}
			}

			for (int y = bY - 1; y <= bY + 2; y++) {
				Location l = new Location(centerBlock.getWorld(), bX, y, bZ);
				selectedBlocks.add(l);
			}

			for (int z = bZ - 1; z <= bZ + 1; z++) {
				for (int y = bY - 1; y <= bY + 1; y++) {
					Location l = new Location(centerBlock.getWorld(), bX, y, z);
					selectedBlocks.add(l);
				}
			}

			for (int x = bX - 1; x <= bX + 1; x++) {
				for (int z = bZ - 1; z <= bZ + 1; z++) {
					Location l = new Location(centerBlock.getWorld(), x, bY, z);
					selectedBlocks.add(l);
				}
			}

			return selectedBlocks;
		}
	}

	// Wait for the frozen blocks to melt and remove it from bendable water list.
	private class SnowMeltingState implements State {
		SnowMeltingState() {
			bPlayer.addCooldown(FrostBreath.this);
		}

		@Override
		public boolean update() {
			return !frozenBlocks.isEmpty();
		}
	}

	private static class FrozenBlock {
		TempBlock tempBlock;
		long endTime;

		FrozenBlock(TempBlock tempBlock, long endTime) {
			this.tempBlock = tempBlock;
			this.endTime = endTime;
		}
	}

	public static class Config {
		long cooldown;
		long duration;
		int particles;
		int freezeDuration;
		int snowDuration;
		int frozenWaterDuration;
		int range;
		boolean snowEnabled;
		boolean bendSnow;
		boolean damageEnabled;
		double playerDamage;
		double mobDamage;
		boolean slowEnabled;
		long slowDuration;
		boolean restrictBiomes;

		Config(Player player) {
			ConfigurationSection config = JedCoreConfig.getConfig(player);

			cooldown = config.getLong("Abilities.Water.FrostBreath.Cooldown");
			duration = config.getLong("Abilities.Water.FrostBreath.Duration");
			particles = config.getInt("Abilities.Water.FrostBreath.Particles");
			freezeDuration = config.getInt("Abilities.Water.FrostBreath.FrostDuration");
			snowDuration = config.getInt("Abilities.Water.FrostBreath.SnowDuration");
			frozenWaterDuration = config.getInt("Abilities.Water.FrostBreath.FrozenWaterDuration");
			range = config.getInt("Abilities.Water.FrostBreath.Range");
			snowEnabled = config.getBoolean("Abilities.Water.FrostBreath.Snow");
			bendSnow = config.getBoolean("Abilities.Water.FrostBreath.BendableSnow");
			damageEnabled = config.getBoolean("Abilities.Water.FrostBreath.Damage.Enabled");
			playerDamage = config.getDouble("Abilities.Water.FrostBreath.Damage.Player");
			mobDamage = config.getDouble("Abilities.Water.FrostBreath.Damage.Mob");
			slowEnabled = config.getBoolean("Abilities.Water.FrostBreath.Slow.Enabled");
			slowDuration = config.getLong("Abilities.Water.FrostBreath.Slow.Duration");
			restrictBiomes = config.getBoolean("Abilities.Water.FrostBreath.RestrictBiomes");
		}
	}
}
