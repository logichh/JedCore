package com.jedk1.jedcore.ability.earthbending;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;
import com.projectkorra.projectkorra.ability.MetalAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.region.RegionProtection;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class MetalShred extends MetalAbility implements AddonAbility {

	@Attribute(Attribute.SELECT_RANGE)
	private int selectRange;
	private int extendTick;
	@Attribute(Attribute.DAMAGE)
	private double damage;

	private boolean horizontal = false;
	private boolean started = false;
	private boolean stop = false;
	private boolean stopCoil = false;
	private boolean extending = false;
	private int length = 0;
	private int fullLength = 0;
	private long lastExtendTime;
	private Block source;
	private Block lastBlock;
	private final List<TempBlock> tblocks = new ArrayList<>();

	public MetalShred(Player player) {
		super(player);

		if (hasAbility(player, MetalShred.class)) {
			getAbility(player, MetalShred.class).remove();
		}

		if (!bPlayer.canBend(this)) {
			return;
		}

		setFields();

		if (selectSource()) {
			if (horizontal) {
				raiseBlock(source, GeneralMethods.getDirection(player.getLocation(), source.getLocation()));
			} else {
				shiftBlock(source, GeneralMethods.getDirection(player.getLocation(), source.getLocation()));
			}

			start();
		}
	}
	
	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		
		selectRange = config.getInt("Abilities.Earth.MetalShred.SourceRange");
		extendTick = config.getInt("Abilities.Earth.MetalShred.ExtendTick");
		damage = config.getDouble("Abilities.Earth.MetalShred.Damage");
	}

	public boolean selectSource() {
		Block b = BlockSource.getEarthSourceBlock(player, selectRange, ClickType.SHIFT_DOWN);

		if (EarthAbility.getMovedEarth().containsKey(b))
			return false;

		if (!isMetal(b))
			return false;

		source = b;

		if (ElementalAbility.isAir(source.getRelative(BlockFace.UP).getType()) && !isMetal(source.getRelative(BlockFace.DOWN))) {
			horizontal = true;
		}

		return true;
	}

	public void raiseBlock(Block b, Vector d) {
		Block up = b.getRelative(BlockFace.UP);
		Block away = b.getRelative(GeneralMethods.getCardinalDirection(d));
		Block awayup = away.getRelative(BlockFace.UP);
		Block deeperb = b.getRelative(BlockFace.DOWN);
		Block deepera = away.getRelative(BlockFace.DOWN);

		for (TempBlock tb : tblocks) {
			if (!ElementalAbility.isAir(tb.getBlock().getType()))
				tb.setType(Material.AIR);
		}

		if (!up.getType().isSolid()) {
			TempBlock tbu = new TempBlock(up, b.getBlockData());
			tblocks.add(tbu);
		}

		if (!awayup.getType().isSolid()) {
			TempBlock tbau = new TempBlock(awayup, away.getBlockData());
			tblocks.add(tbau);
		}

		if (isMetal(b)) {
			TempBlock tbd = new TempBlock(b, Material.AIR.createBlockData());
			tblocks.add(tbd);
		}

		if (isMetal(away)) {
			TempBlock tba = new TempBlock(away, Material.AIR.createBlockData());
			tblocks.add(tba);
		}

		if (isMetal(deeperb)) {
			TempBlock tbdb = new TempBlock(deeperb, Material.AIR.createBlockData());
			tblocks.add(tbdb);
		}

		if (isMetal(deepera)) {
			TempBlock tbda = new TempBlock(deepera, Material.AIR.createBlockData());
			tblocks.add(tbda);
		}

		playMetalbendingSound(b.getLocation());
	}

	public void shiftBlock(Block b, Vector d) {
		Block under = b.getRelative(BlockFace.DOWN);
		Block side = b.getRelative(GeneralMethods.getCardinalDirection(d).getOppositeFace());
		Block underside = under.getRelative(GeneralMethods.getCardinalDirection(d).getOppositeFace());

		for (TempBlock tb : tblocks) {
			if (!ElementalAbility.isAir(tb.getBlock().getType()))
				tb.setType(Material.AIR);
		}

		if (!side.getType().isSolid()) {
			TempBlock tbs = new TempBlock(side, b.getBlockData());
			tblocks.add(tbs);
		}

		if (!underside.getType().isSolid()) {
			TempBlock tbus = new TempBlock(underside, under.getBlockData());
			tblocks.add(tbus);
		}

		if (isMetal(b)) {
			TempBlock tb1 = new TempBlock(b, Material.AIR.createBlockData());
			tblocks.add(tb1);
		}

		if (isMetal(under)) {
			TempBlock tb2 = new TempBlock(under, Material.AIR.createBlockData());
			tblocks.add(tb2);
		}

		playMetalbendingSound(b.getLocation());
	}

	private void peelCoil(Block b) {
		Block under = b.getRelative(BlockFace.DOWN);

		if (length <= 0)
			return;

		if (!b.getType().isSolid()) {
			TempBlock tbb = new TempBlock(b, Material.IRON_BLOCK.createBlockData());
			tblocks.add(tbb);
		}

		else
			stopCoil = true;

		if (!under.getType().isSolid()) {
			TempBlock tbu = new TempBlock(under, Material.IRON_BLOCK.createBlockData());
			tblocks.add(tbu);
		}

		else
			stopCoil = true;

		playMetalbendingSound(b.getLocation());

		length--;
	}

	public static void startShred(Player player) {
		if (hasAbility(player, MetalShred.class)) {
			getAbility(player, MetalShred.class).startShred();
		}
	}

	private void startShred() {
		started = true;
	}

	public static void extend(Player player) {
		if (hasAbility(player, MetalShred.class)) {
			getAbility(player, MetalShred.class).extend();
		}
	}

	private void extend() {
		if (extending) {
			extending = false;
			return;
		}

		if (!stop)
			return;

		lastExtendTime = System.currentTimeMillis();
		fullLength = length;
		if (lastBlock != null)
			lastBlock = lastBlock.getRelative(GeneralMethods.getCardinalDirection(GeneralMethods.getDirection(player.getLocation(), lastBlock.getLocation())).getOppositeFace());
		else {
			return;
		}
		extending = true;
	}

	@Override
	public void progress() {
		if (!player.isOnline() || player.isDead()) {
			remove();
			return;
		}

		if (!bPlayer.canBendIgnoreCooldowns(this)) {
			remove();
			return;
		}

		if (!player.isSprinting()) {
			if (started)
				stop = true;
		}

		if (!horizontal && stop && !stopCoil && extending && System.currentTimeMillis() > lastExtendTime + extendTick) {
			lastExtendTime = System.currentTimeMillis();
			if (length > 0) {

				Block b = lastBlock.getRelative(GeneralMethods.getCardinalDirection(GeneralMethods.getDirection(lastBlock.getLocation(), GeneralMethods.getTargetedLocation(player, fullLength))));

				peelCoil(b);

				for (Entity e : GeneralMethods.getEntitiesAroundPoint(b.getLocation(), 2)) {
					if (!(e instanceof LivingEntity) || e.getEntityId() == player.getEntityId()) {
						continue;
					}
					if (RegionProtection.isRegionProtected(this, e.getLocation()) || ((e instanceof Player) && Commands.invincible.contains(e.getName()))) {
						continue;
					}
					DamageHandler.damageEntity(e, damage, this);
					GeneralMethods.setVelocity(this, e, e.getVelocity().add(player.getLocation().getDirection().add(new Vector(0, 0.1, 0))));
				}

				lastBlock = b;
			}

			return;
		}

		if (stop || !started)
			return;

		Block b;

		if (lastBlock != null) {
			b = lastBlock.getRelative(GeneralMethods.getCardinalDirection(player.getLocation().getDirection()));
		}

		else {
			b = source.getRelative(GeneralMethods.getCardinalDirection(player.getLocation().getDirection()));
		}

		if (!isMetal(b)) {
			if (!ElementalAbility.isAir(b.getType())) {
				remove();
			}
			return;
		}

		if (b.getLocation().getX() == player.getLocation().getBlockX() || b.getLocation().getZ() == player.getLocation().getBlockZ()) {
			if (horizontal)
				raiseBlock(b, GeneralMethods.getDirection(player.getLocation(), b.getLocation()));
			else
				shiftBlock(b, GeneralMethods.getDirection(player.getLocation(), b.getLocation()));

			length++;
			lastBlock = b;
		}
	}

	private void revertAll() {
		for (TempBlock tb : tblocks) {
			tb.revertBlock();
		}
	}

	@Override
	public void remove() {
		revertAll();
		super.remove();
	}
	
	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "MetalShred";
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
		return "* JedCore Addon *\n" + config.getString("Abilities.Earth.MetalShred.Description");
	}

	public int getSelectRange() {
		return selectRange;
	}

	public void setSelectRange(int selectRange) {
		this.selectRange = selectRange;
	}

	public int getExtendTick() {
		return extendTick;
	}

	public void setExtendTick(int extendTick) {
		this.extendTick = extendTick;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public boolean isHorizontal() {
		return horizontal;
	}

	public void setHorizontal(boolean horizontal) {
		this.horizontal = horizontal;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public boolean isStopCoil() {
		return stopCoil;
	}

	public void setStopCoil(boolean stopCoil) {
		this.stopCoil = stopCoil;
	}

	public boolean isExtending() {
		return extending;
	}

	public void setExtending(boolean extending) {
		this.extending = extending;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getFullLength() {
		return fullLength;
	}

	public void setFullLength(int fullLength) {
		this.fullLength = fullLength;
	}

	public long getLastExtendTime() {
		return lastExtendTime;
	}

	public void setLastExtendTime(long lastExtendTime) {
		this.lastExtendTime = lastExtendTime;
	}

	public Block getSource() {
		return source;
	}

	public void setSource(Block source) {
		this.source = source;
	}

	public Block getLastBlock() {
		return lastBlock;
	}

	public void setLastBlock(Block lastBlock) {
		this.lastBlock = lastBlock;
	}

	public List<TempBlock> getTblocks() {
		return tblocks;
	}

	@Override
	public void load() {}

	@Override
	public void stop() {}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Earth.MetalShred.Enabled");
	}
}
