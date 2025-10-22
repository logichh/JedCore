package com.jedk1.jedcore.ability.firebending;

import com.jedk1.jedcore.JCMethods;
import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.collision.CollisionDetector;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.FireTick;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.BlueFireAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FireSki extends FireAbility implements AddonAbility {

	private Location location;

	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long duration;
	@Attribute(Attribute.SPEED)
	private double speed;
	private boolean ignite;
	@Attribute(Attribute.FIRE_TICK)
	private int fireTicks;
	private double requiredHeight;

	public FireSki(Player player) {
		super(player);
		if (!isEnabled()) {
			return;
		}
		
		if (hasAbility(player, FireSki.class)) {
			FireSki fs = getAbility(player, FireSki.class);
			fs.remove();
			return;
		}
		
		if (!bPlayer.canBend(getAbility("FireJet"))) {
			return;
		}

		setFields();

		if (CollisionDetector.isOnGround(player) || CollisionDetector.distanceAboveGround(player) < requiredHeight) {
			return;
		}
		
		this.flightHandler.createInstance(player, this.getName());

		location = player.getLocation();
		player.setAllowFlight(true);
		player.setFlying(true);

		bPlayer.addCooldown(getAbility("FireJet"), getCooldown());
		start();
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Fire.FireSki.Cooldown");
		duration = config.getLong("Abilities.Fire.FireSki.Duration");
		speed = config.getDouble("Abilities.Fire.FireSki.Speed");
		ignite = config.getBoolean("Abilities.Fire.FireSki.IgniteEntities");
		fireTicks = config.getInt("Abilities.Fire.FireSki.FireTicks");
		requiredHeight = config.getDouble("Abilities.Fire.FireSki.RequiredHeight");
		
		applyModifiers();
	}
	
	private void applyModifiers() {
		if (bPlayer.canUseSubElement(SubElement.BLUE_FIRE)) {
			cooldown *= BlueFireAbility.getCooldownFactor();
		}
		
		if (isDay(player.getWorld())) {
			cooldown -= ((long) getDayFactor(cooldown) - cooldown);
		}
	}

	private void allowFlight() {
		player.setAllowFlight(true);
		player.setFlying(true);
	}

	private void removeFlight() {
		player.setAllowFlight(false);
		player.setFlying(false);
	}

	@Override
	public void progress() {
		if (player.isDead() || !player.isOnline()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreCooldowns(getAbility("FireJet"))) {
			remove();
			return;
		}
		if (!collision()) {
			movePlayer();
			if (System.currentTimeMillis() > getStartTime() + duration || isWater(player.getLocation().getBlock())) {
				remove();
			}
		} else {
			remove();
		}
	}

	private void movePlayer() {
		location = player.getEyeLocation();
		location.setPitch(0);
		Vector dV = location.getDirection().normalize();
		Vector travel;

		if (getPlayerDistance() > 1.8) {
			removeFlight();
			travel = new Vector(dV.getX() * speed, -0.09, dV.getZ() * speed);
		} else if (getPlayerDistance() < 1.7) {
			allowFlight();
			travel = new Vector(dV.getX() * speed, 0.2, dV.getZ() * speed);
		} else {
			travel = new Vector(dV.getX() * speed, 0, dV.getZ() * speed);
		}

		playFirebendingSound(player.getLocation());
		createBeam();

		if (ignite) {
			for (Entity entity : GeneralMethods.getEntitiesAroundPoint(player.getLocation().clone().add(0, -1, 0), 2.0)) {
				if (entity instanceof LivingEntity && entity.getEntityId() != player.getEntityId()) {
					FireTick.set(entity, this.fireTicks);
				}
			}
		}

		GeneralMethods.setVelocity(this, player, travel);
		player.setFallDistance(0);
	}

	private double getPlayerDistance() {
		Location l = player.getLocation().clone();
		while (l.getBlockY() > l.getWorld().getMinHeight() && !GeneralMethods.isSolid(l.getBlock())) {
			l.add(0, -0.1, 0);
		}
		return player.getLocation().getY() - l.getY();
	}

	private void createBeam() {
		Location right = player.getEyeLocation().add(getRightHeadDirection(player).multiply(1.75));
		right.setPitch(-15);
		Location right1 = right.subtract(right.getDirection().multiply(4)).add(0, -1.5, 0);

		Location left = player.getEyeLocation().add(getLeftHeadDirection(player).multiply(1.75));
		left.setPitch(-15);
		Location left1 = left.subtract(left.getDirection().multiply(4)).add(0, -1.5, 0);

		double size = 0;

		for (Location l : JCMethods.getLinePoints(player.getEyeLocation().add(0, -0.5, 0).add(getRightHeadDirection(player).multiply(0.2)), right1, 6)) {
			size += 0.05;
			playFirebendingParticles(l, 4, (Math.random() * size + 0.01), (Math.random() * size + 0.01), (Math.random() * size + 0.01));
			ParticleEffect.SMOKE_NORMAL.display(l, 1, (Math.random() * size + 0.01), (Math.random() * size + 0.01), (Math.random() * size + 0.01), 0.08);
		    JCMethods.emitLight(l);
		}

		size = 0;
		for (Location l : JCMethods.getLinePoints(player.getEyeLocation().add(0, -0.5, 0).add(getLeftHeadDirection(player).multiply(0.2)), left1, 6)) {
			size += 0.05;
			playFirebendingParticles(l, 4, (Math.random() * size + 0.01), (Math.random() * size + 0.01), (Math.random() * size + 0.01));
			ParticleEffect.SMOKE_NORMAL.display(l, 1, (Math.random() * size + 0.01), (Math.random() * size + 0.01), (Math.random() * size + 0.01), 0.08);
			JCMethods.emitLight(l);
		}
	}

	public Vector getRightHeadDirection(Player player) {
		Vector direction = player.getLocation().getDirection().normalize();
		return new Vector(-direction.getZ(), 0.0, direction.getX()).normalize();
	}

	public Vector getLeftHeadDirection(Player player) {
		Vector direction = player.getLocation().getDirection().normalize();
		return new Vector(direction.getZ(), 0.0, -direction.getX()).normalize();
	}

	private boolean collision() {
		Location l = player.getEyeLocation();
		l.setPitch(0);
		Vector dV = l.getDirection().normalize();
		l.add(new Vector(dV.getX() * 0.8, 0, dV.getZ() * 0.8));

		if (l.getBlock().getType().isSolid()) {
			return true;
		}
		if (l.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
			return true;
		}
		return l.clone().add(0, -2, 0).getBlock().getType().isSolid();
	}

	@Override
	public void remove() {
		removeFlight();

		this.flightHandler.removeInstance(player, this.getName());

		super.remove();
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
		return "FireSki";
	}

	@Override
	public boolean isHiddenAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
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
		return null;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public boolean isIgnite() {
		return ignite;
	}

	public void setIgnite(boolean ignite) {
		this.ignite = ignite;
	}

	public int getFireTicks() {
		return fireTicks;
	}

	public void setFireTicks(int fireTicks) {
		this.fireTicks = fireTicks;
	}

	public double getRequiredHeight() {
		return requiredHeight;
	}

	public void setRequiredHeight(double requiredHeight) {
		this.requiredHeight = requiredHeight;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Fire.FireSki.Enabled");
	}

	public static boolean isPunchActivated(World world) {
		ConfigurationSection config = JedCoreConfig.getConfig(world);
		return config.getBoolean("Abilities.Fire.FireSki.PunchActivated");
	}
}
