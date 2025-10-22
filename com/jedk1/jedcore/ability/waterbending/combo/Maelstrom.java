package com.jedk1.jedcore.ability.waterbending.combo;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.configuration.JedCoreConfig;
import com.jedk1.jedcore.util.RegenTempBlock;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.ability.util.ComboUtil;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.waterbending.Torrent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Maelstrom extends WaterAbility implements AddonAbility, ComboAbility {

	private int depth;
	@Attribute(Attribute.RANGE)
	private int range;
	@Attribute(Attribute.COOLDOWN)
	private long cooldown;
	@Attribute(Attribute.DURATION)
	private long duration;

	private final List<Block> pool = new ArrayList<>();
	private final List<Block> wave = new ArrayList<>();
	private final List<Block> changedBlocks = new ArrayList<>();
	private Location origin;
	private int step;
	private int levelStep;
	private int angle;
	private boolean canRemove;

	public Maelstrom(Player player) {
		super(player);
		if (!bPlayer.canBendIgnoreBinds(this) || hasAbility(player, Maelstrom.class)) {
			return;
		}
		setFields();
		if (setOrigin()) {
			start();
			if (!isRemoved()) {
				bPlayer.addCooldown(this);
				Torrent t = getAbility(player, Torrent.class);
				if (t != null) {
					t.remove();
				}
			}
		}
	}

	public void setFields() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);

		cooldown = config.getLong("Abilities.Water.WaterCombo.Maelstrom.Cooldown");
		duration = config.getLong("Abilities.Water.WaterCombo.Maelstrom.Duration");
		depth = config.getInt("Abilities.Water.WaterCombo.Maelstrom.MaxDepth");
		range = config.getInt("Abilities.Water.WaterCombo.Maelstrom.Range");
		canRemove = true;
		
		applyModifiers();
	}
	
	private void applyModifiers() {
		if (isNight(player.getWorld())) {
			cooldown -= ((long) getNightFactor(cooldown) - cooldown);
			range = (int) getNightFactor(range);
		}
	}

	public boolean setOrigin() {
		Block block = BlockSource.getWaterSourceBlock(player, range, ClickType.LEFT_CLICK, true, false, false);
		if (block != null) {
			if (!isTransparent(block.getRelative(BlockFace.UP))) {
				return false;
			}
			for (int i = 0; i < depth; i++) {
				if (!isWater(block.getRelative(BlockFace.DOWN, i))) {
					setDepth(i - 1);
					break;
				}
			}
			if (getDepth() < 3) {
				return false;
			}
			origin = block.getLocation().clone();
			for (Location l : GeneralMethods.getCircle(origin, getDepth(), 1, false, false, 0)) {
				if (!isWater(l.getBlock())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void progress() {
		if (player == null || !player.isOnline() || player.getWorld() != origin.getWorld()) {
			remove();
			return;
		}
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			remove();
			return;
		}
		if (System.currentTimeMillis() > getStartTime() + duration) {
			remove();
			return;
		}
		removeWater(false);
		playAnimation();
		dragEntities();
		if (canRemove && (step == 0 || step % 20 == 0)) {
			if (levelStep < getDepth()) {
				levelStep++;
				removeWater(true);
			}
			if (step == 20) {
				step = 0;
			}
		}
		step++;
	}

	public void removeWater(boolean increase) {
		if (increase) {
			pool.clear();
			for (int i = 0; i < levelStep; i++) {
				for (Location l : GeneralMethods.getCircle(origin.clone().subtract(0, i, 0), levelStep - i, 1, false, false, 0)) {
					if (!isWater(l.getBlock()) && !isTransparent(l.getBlock())) {
						canRemove = false;
						break;
					}
					if (!pool.contains(l.getBlock())) {
						pool.add(l.getBlock());
					}
				}
			}
		}
		for (Block b : pool) {
			if (wave.contains(b)) continue;
			if (!changedBlocks.contains(b)) {
				changedBlocks.add(b);
			}
			new RegenTempBlock(b, Material.AIR, Material.AIR.createBlockData(), 100);
		}
	}

	public void dragEntities(){
		for(Block b : pool){
			if (pool.indexOf(b) % 3 == 0) {
				Location l = b.getLocation();
				for(Entity entity : GeneralMethods.getEntitiesAroundPoint(l, 1.5D)){
					Vector direction = GeneralMethods.getDirection(entity.getLocation(), origin.clone().subtract(-0.5, (levelStep - 1), -0.5));
					entity.setVelocity(direction.multiply(0.2));
				}
			}
		}
	}

	public void playAnimation() {
		wave.clear();
		int waves = 5;
		int newAngle = this.angle;
		for (int i = 0; i < levelStep; i++) {
			for (int degree = 0; degree < waves; degree++) {
				double size = (levelStep - i) - 1;
				double angle = ((newAngle + (degree * (360F / waves))) * Math.PI / 180);
				double x = size * Math.cos(angle);
				double z = size * Math.sin(angle);
				Location loc = origin.clone();
				loc.add(x + 0.5, -(i - 0.5), z + 0.5);
				Block b = loc.getBlock();
				for (int j = 0; j < 2; j++) {
					Block blockToChange = b.getRelative(BlockFace.DOWN, j);
					wave.add(blockToChange);
					if (!changedBlocks.contains(blockToChange)) {
						changedBlocks.add(blockToChange);
					}
					new RegenTempBlock(blockToChange, Material.WATER, Material.WATER.createBlockData(bd -> ((Levelled) bd).setLevel(1)), 0);
					ParticleEffect.WATER_SPLASH.display(loc, 3, Math.random(), Math.random(), Math.random(), 0);
				}
			}
			newAngle += 15;
		}
		this.angle+=(levelStep * 2);

	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getDepth() {
		return depth;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return origin;
	}

	@Override
	public String getName() {
		return "Maelstrom";
	}

	@Override
	public boolean isHiddenAbility() {
		return false;
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
	public Object createNewComboInstance(Player player) {
		return new Maelstrom(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		return ComboUtil.generateCombinationFromList(this, JedCoreConfig.getConfig(player).getStringList("Abilities.Water.WaterCombo.Maelstrom.Combination"));
	}

	@Override
	public String getInstructions() {
		return JedCoreConfig.getConfig(player).getString("Abilities.Water.WaterCombo.Maelstrom.Instructions");
	}

	@Override
	public String getDescription() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return "* JedCore Addon *\n" + config.getString("Abilities.Water.WaterCombo.Maelstrom.Description");
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
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

	public List<Block> getPool() {
		return pool;
	}

	public List<Block> getWave() {
		return wave;
	}

	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(Location origin) {
		this.origin = origin;
	}

	public int getAngle() {
		return angle;
	}

	public void setAngle(int angle) {
		this.angle = angle;
	}

	public boolean canRemove() {
		return canRemove;
	}

	public void setCanRemove(boolean canRemove) {
		this.canRemove = canRemove;
	}

	@Override
	public void load() {}

	@Override
	public void remove() {
		revertAllBlocks();
		super.remove();
	}
	
	@Override
	public void stop() {
		revertAllBlocks();
	}
	
	private void revertAllBlocks() {
		for (Block block : changedBlocks) {
			if (block != null && block.getWorld() != null) {
				RegenTempBlock.revert(block);
			}
		}
		changedBlocks.clear();
	}

	@Override
	public boolean isEnabled() {
		ConfigurationSection config = JedCoreConfig.getConfig(this.player);
		return config.getBoolean("Abilities.Water.WaterCombo.Maelstrom.Enabled");
	}
}
