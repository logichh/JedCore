package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MetalHook extends MetalAbility implements AddonAbility {

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute("MaxHooks")
	private int maxHooks;
	private int totalHooks;
	private int hooksUsed;
	private boolean noSource;
	private boolean barrierHooking;

	private boolean hasHook;
	private boolean wasSprinting;
	private long time;

	private Location destination;

	private final ConcurrentHashMap<Arrow, Boolean> hooks = new ConcurrentHashMap<>();
	private final List<UUID> hookIds = new ArrayList<>();

	public MetalHook(Player player) {
		super(player);

		if (!bPlayer.canBend(this) || !bPlayer.canMetalbend()) {
			return;
		}

		if (hasAbility(player, MetalHook.class)) {
			MetalHook mh = getAbility(player, MetalHook.class);
			mh.launchHook();
			return;
		}

		setFields();

		if (!hasRequiredInv()) {
			return;
		}

		wasSprinting = player.isSprinting();
		flightHandler.createInstance(player, this.getName());
		player.setAllowFlight(true);

		start();
		launchHook();
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		cooldown = config.getLong("Abilities.Earth.MetalHook.Cooldown");
		range = config.getInt("Abilities.Earth.MetalHook.Range");
		maxHooks = config.getInt("Abilities.Earth.MetalHook.MaxHooks");
		totalHooks = config.getInt("Abilities.Earth.MetalHook.TotalHooks");
		noSource = config.getBoolean("Abilities.Earth.MetalHook.RequireItem");
		barrierHooking = config.getBoolean("Abilities.Earth.MetalHook.BarrierHooking");
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline() || player.isDead()) {
			removeAllArrows();
			remove();
			return;
		}

		if (!bPlayer.canBendIgnoreBindsCooldowns(this) || hooks.isEmpty()) {
			removeAllArrows();
			remove();
			return;
		}

		if (!wasSprinting && player.isSprinting()) {
			removeAllArrows();
			remove();
			return;
		}

		wasSprinting = player.isSprinting();

		if (player.isSneaking()) {
			player.setVelocity(new Vector());

			if (System.currentTimeMillis() > (time + 1000)) {
				removeAllArrows();
				remove();
				return;
			}
		} else {
			time = System.currentTimeMillis();
		}

		Vector target = new Vector();

		for (Arrow a : hooks.keySet()) {
			if (a != null) {
				if (!isActiveArrow(a)) {
					hooks.remove(a);
					hookIds.remove(a.getUniqueId());
					a.remove();
					continue;
				}

				if (a.getAttachedBlock() == null) {
					hooks.replace(a, hooks.get(a), false);
				} else {
					hooks.replace(a, hooks.get(a), true);
					hasHook = true;
				}
				
				//Draws the particle lines.
				for (Location location : JCMethods.getLinePoints(player.getLocation().add(0, 1, 0), a.getLocation(), ((int) player.getLocation().add(0,1,0).distance(a.getLocation()) * 2))) {
					GeneralMethods.displayColoredParticle("#CCCCCC", location);
				}

				if (hooks.get(a)) {
					target.add(GeneralMethods.getDirection(player.getEyeLocation(), a.getLocation()));
				}
			}
		}

		if (hasHook) {
			destination = player.getLocation().clone().add(target);

			if (player.getLocation().distance(destination) > 2) {
				player.setFlying(false);
				double velocity = 0.8;

				GeneralMethods.setVelocity(this, player, target.clone().normalize().multiply(velocity));
			} else if (player.getLocation().distance(destination) < 2 && player.getLocation().distance(destination) >= 1) {
				player.setFlying(false);
				double velocity = 0.35;

				GeneralMethods.setVelocity(this, player, target.clone().normalize().multiply(velocity));
			} else {
				GeneralMethods.setVelocity(this, player, new Vector(0, 0, 0));

				if (player.getAllowFlight()) {
					player.setFlying(true);
				}
			}
		}
	}

	private boolean isActiveArrow(Arrow arrow) {
		if (arrow.isDead()) return false;
		if (player.getWorld() != arrow.getWorld()) return false;

		Block attached = arrow.getAttachedBlock();

		if (!barrierHooking && attached != null && attached.getType() == Material.BARRIER) return false;

		return player.getEyeLocation().distanceSquared(arrow.getLocation()) < range * range;
	}

	@Override
	public void remove() {
		if (player.isOnline()) {
			bPlayer.addCooldown(this);
		}

		flightHandler.removeInstance(player, this.getName());

		super.remove();
	}

	public void launchHook() {
		if (!hasRequiredInv()) return;

		Vector dir = GeneralMethods.getDirection(player.getEyeLocation(), GeneralMethods.getTargetedLocation(player, range));

		if (!hookIds.isEmpty() && hookIds.size() > (maxHooks - 1)) {
			for (Arrow a : hooks.keySet()) {
				if (a.getUniqueId().equals(hookIds.get(0))) {
					hooks.remove(a);
					hookIds.remove(0);
					a.remove();
					break;
				}
			}
		}

		if (totalHooks > 0 && hooksUsed++ > totalHooks) {
			remove();
			return;
		}

		Arrow a = player.getWorld().spawnArrow(player.getEyeLocation().add(player.getLocation().getDirection().multiply(2)), dir, 3, 0f);
		a.setMetadata("metalhook", new FixedMetadataValue(JedCore.plugin, "1"));

		hooks.put(a, false);
		hookIds.add(a.getUniqueId());
	}

	public void removeAllArrows() {
		for (Arrow a : hooks.keySet()) {
			a.remove();
		}
	}

	public boolean hasRequiredInv() {
		if (noSource) return true;

		if (player.getInventory().getChestplate() != null) {
			Material[] chestplates = {Material.IRON_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE};
			Material playerChest = player.getInventory().getChestplate().getType();

			if (Arrays.asList(chestplates).contains(playerChest)) {
				return true;
			}
		}

		Material[] metals = {Material.IRON_INGOT, Material.IRON_BLOCK};

		for (ItemStack items : player.getInventory()) {
			if (items != null && Arrays.asList(metals).contains(items.getType())) {
				return true;
			}
		}

		return false;
	}

	public int getMaxHooks() {
		return this.maxHooks;
	}

	public void setMaxHooks(int maxhooks) {
		this.maxHooks = maxhooks;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "MetalHook";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.MetalHook.Description");
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public int getTotalHooks() {
		return totalHooks;
	}

	public void setTotalHooks(int totalHooks) {
		this.totalHooks = totalHooks;
	}

	public int getHooksUsed() {
		return hooksUsed;
	}

	public void setHooksUsed(int hooksUsed) {
		this.hooksUsed = hooksUsed;
	}

	public boolean isNoSource() {
		return noSource;
	}

	public void setNoSource(boolean noSource) {
		this.noSource = noSource;
	}

	public boolean isBarrierHooking() {
		return barrierHooking;
	}

	public void setBarrierHooking(boolean barrierHooking) {
		this.barrierHooking = barrierHooking;
	}

	public boolean isHasHook() {
		return hasHook;
	}

	public void setHasHook(boolean hasHook) {
		this.hasHook = hasHook;
	}

	public boolean isWasSprinting() {
		return wasSprinting;
	}

	public void setWasSprinting(boolean wasSprinting) {
		this.wasSprinting = wasSprinting;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public Location getDestination() {
		return destination;
	}

	public void setDestination(Location destination) {
		this.destination = destination;
	}

	public ConcurrentHashMap<Arrow, Boolean> getHooks() {
		return hooks;
	}

	public List<UUID> getHookIds() {
		return hookIds;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.MetalHook.Enabled");
	}
}
