package com.jedk1.jedcore;

import com.jedk1.jedcore.ability.earthbending.EarthPillar;
import com.jedk1.jedcore.ability.waterbending.IceWall;
import com.jedk1.jedcore.ability.firebending.LightningBurst;
import com.jedk1.jedcore.ability.waterbending.HealingWaters;
import com.jedk1.jedcore.ability.waterbending.IcePassive;
import com.jedk1.jedcore.util.RegenTempBlock;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class JCManager implements Runnable {

	public JedCore plugin;
	
	public JCManager(JedCore plugin) {
		this.plugin = plugin;
	}
	
	public void run() {
		// Process abilities using ProjectKorra's ThreadUtil for Folia compatibility
		// Process abilities that need to run on the main thread
		LightningBurst.progressAll();
		HealingWaters.heal(Bukkit.getServer());
		IcePassive.handleSkating();
		IceWall.progressAll();
		EarthPillar.progressAll();
		RegenTempBlock.manage();
	}
}