package com.github.soerxpso.xpspawners.listeners;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.soerxpso.xpspawners.Spawner;
import com.github.soerxpso.xpspawners.XPSpawners;
import com.github.soerxpso.xpspawners.manager.ConfigManager;
import com.github.soerxpso.xpspawners.manager.SpawnerManager;

public class BlockListener implements Listener {
	
	SpawnerManager spawnerManager;
	
	public BlockListener() {
		spawnerManager = XPSpawners.getPlugin().getSpawnerManager();
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent e) {
		activateSpawner(e.getPlayer(), e.getBlock());
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockInteract(PlayerInteractEvent e) {
		activateSpawner(e.getPlayer(), e.getClickedBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		Block b = e.getBlock();
		if (b == null) return;

		//the item the block was broken with
		ItemStack is = e.getPlayer() != null ? e.getPlayer().getInventory().getItemInMainHand(): null;
		
		boolean brokenWithSilk = (is != null && is.getEnchantments() != null ? is.getEnchantments().containsKey(Enchantment.SILK_TOUCH) : false);
		boolean isXpSpawner = Material.MOB_SPAWNER.equals(b.getType());
		
		if(isXpSpawner) {
			removeSpawner(b);
			e.setCancelled(true);
			e.getBlock().setType(Material.AIR);
			if(brokenWithSilk) {
				if(Math.random() < ConfigManager.getSilkDropChance()) {
					dropSpawnerLater(b);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent e) {
		for (Block b : e.blockList()) {
			if(b != null && Material.MOB_SPAWNER.equals(b.getType())) {
				removeSpawner(b);
			}
		}
	}
	
	private void activateSpawner(Player p, Block b) {
		if(b == null || !Material.MOB_SPAWNER.equals(b.getType())) {
			return;
		}
		
		Spawner spawner = spawnerManager.getSpawner(b.getLocation());
		
		if(spawner == null) {
			BlockState bs = b.getState();
			if (bs instanceof CreatureSpawner) {
				spawnerManager.addSpawner(new Spawner((CreatureSpawner)bs));
				
				if(p != null) {
					p.sendMessage(ChatColor.GREEN + "XP Spawner is activated.");
				}
			} else {
				XPSpawners.getPlugin().getLogger().log(Level.INFO, "Attempted to activate spawner but it is not a Creature Spawner {0}", 
						b.getLocation());
			}
		} else if(p != null) {
			if(spawner.getLocation().equals(b.getLocation())) {
				p.sendMessage(ChatColor.YELLOW + "XP Spawner is already activated.");
			} else {
				p.sendMessage(ChatColor.RED + "Another XP Spawner is already activated in this area.");
			}
		}
	}
	
	private void removeSpawner(Block b) {
		Spawner spawner = spawnerManager.getSpawner(b.getLocation());
		if(spawner != null) {
			spawnerManager.removeSpawner(spawner);
		}
	}
	
	private void dropSpawnerLater(Block b) {
		ItemStack itemToDrop = XPSpawners.getPlugin()
				.getSpawnerManager().convertSpawnerToItem(b);
		new BukkitRunnable() {
			@Override
			public void run() {
				b.getWorld().dropItem(b.getLocation().add(0.5, 0.5, 0.5), itemToDrop);
			}
		}.runTaskLater(XPSpawners.getPlugin(), 1);
	}
}
