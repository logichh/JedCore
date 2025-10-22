package com.jedk1.jedcore.listener;

import com.jedk1.jedcore.JedCore;
import com.jedk1.jedcore.command.JedCoreCommand;
import com.jedk1.jedcore.event.PKCommandEvent;
import com.jedk1.jedcore.event.PKCommandEvent.CommandType;
import com.projectkorra.projectkorra.command.PKCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.UUID;

public class CommandListener implements Listener {

	JedCore plugin;
	String[] cmdaliases = {"/bending", "/bend", "/b", "/pk", "/projectkorra", "/korra", "/mtla", "/tla"};
	public static String[] developers = {
			"4eb6315e-9dd1-49f7-b582-c1170e497ab0", //jedk1
			"d57565a5-e6b0-44e3-a026-979d5de10c4d", //s3xi
			"e98a2f7d-d571-4900-a625-483cbe6774fe", //Aztl
			"b6bd2ceb-4922-4707-9173-8a02044e9069" //Cozmyc
	};

	public CommandListener(JedCore plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		String cmd = event.getMessage().toLowerCase();
		String[] args = cmd.split("\\s+");
		if (Arrays.asList(cmdaliases).contains(args[0]) && args.length >= 2) {
			PKCommandEvent new_event = new PKCommandEvent(event.getPlayer(), args, null);
			for (PKCommand command : PKCommand.instances.values()) {
				if (Arrays.asList(command.getAliases()).contains(args[1].toLowerCase())) {
					new_event = new PKCommandEvent(event.getPlayer(), args, CommandType.getType(command.getName()));
				}
			}
			Bukkit.getServer().getPluginManager().callEvent(new_event);
		}
	}

	CommandType[] types = {CommandType.ADD, CommandType.BIND, CommandType.CHOOSE, CommandType.CLEAR, CommandType.PRESET, CommandType.REMOVE};

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPKCommand(final PKCommandEvent event) {
		// Use ProjectKorra's ThreadUtil for command processing
		JedCore.plugin.getServer().getScheduler().scheduleSyncDelayedTask(JedCore.plugin, () -> {
			if (event.getType() != null) {
				if (event.getType().equals(CommandType.WHO) && event.getSender().hasPermission("bending.command.who")) {
					if (event.getArgs().length == 3) {
						if (Bukkit.getPlayer(event.getArgs()[2]) != null) {
							UUID uuid = Bukkit.getPlayer(event.getArgs()[2]).getUniqueId();
							if (Arrays.asList(developers).contains(uuid.toString())) {
								event.getSender().sendMessage(ChatColor.DARK_AQUA + "JedCore Developer");
							}
						}
					}
					return;
				}
				if (event.getType().equals(CommandType.VERSION) && event.getSender().hasPermission("bending.command.version")) {
					JedCoreCommand.sendBuildInfo(event.getSender());
				}
			}
		}, 1);
	}
}
