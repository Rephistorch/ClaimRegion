package org.fightidiocy.Plugins;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Method.MethodAccount;
import com.nijikokun.register.payment.Methods;

public class ClaimRegion extends org.bukkit.plugin.java.JavaPlugin implements Listener
{
	HashMap<String, ProtectedRegion> PendingRegions = new HashMap<String, ProtectedRegion>();
	HashMap<String, Integer> PendingCosts = new HashMap<String, Integer>();
	
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player = null;
		if (cmd.getName().equalsIgnoreCase("claim"))
		{
			if (args.length > 0)
			{
				if (sender instanceof Player)
				{
					player = (Player)sender;
					
					if (args.length == 1)
					{
						int param1 = tryParse(args[0]);
						
						if (args[0].equalsIgnoreCase("help"))
						{
							player.sendMessage("You can use the /Claim command to easily set up a Cube, or a custom cuboid. \nCube: /Claim <size>\nCuboid: /Claim <x> <z> <h> \nX|Z: width on the X|Z axis with your position as the center\nH:Height (from the block you're standing on, upwards).\nSize|X|Z|H must all be greater than or equal to 11.");
							player.sendMessage("You can find the name of your active region by doing /Claim info.");
							player.sendMessage("You can add or remove members by using /Claim RegionName (AddMember|RemoveMember) MemberName.");
						}
						else if (param1 > -1)
						{
							if (param1 > 10)
							{
								StartRegion(player, param1, param1, param1);
							}
							else
							{
								player.sendMessage("Your region must be at least 11 cubed.");
							}
						}
						else if (args[0].equalsIgnoreCase("accept"))
						{
							if (PendingRegions.containsKey(player.getName()))
							{						
								Method m = Methods.getMethod();
								
								if (m.hasAccount(player.getName()))
								{
									MethodAccount acc = m.getAccount(player.getName());
									
									if (acc.balance() >= PendingCosts.get(player.getName()))
									{
										RegionManager mgr = WGBukkit.getRegionManager(player.getWorld());
										if (mgr.getApplicableRegions(PendingRegions.get(player.getName())).size() > 0)
										{
											player.sendMessage("This region is now overlapping another region.  Please try a different location.");
										}
										else
										{
											mgr.addRegion(PendingRegions.get(player.getName()));
											try {
												mgr.save();
												acc.subtract((double)PendingCosts.get(player.getName()));												
												player.sendMessage("Region Created Successfully!");
											} catch (ProtectionDatabaseException e) {
												e.printStackTrace();
												player.sendMessage("Unknown error!");
											}
											PendingRegions.remove(player.getName());
											PendingCosts.remove(player.getName());
										}
									}
									else
									{
										player.sendMessage("Not enough funds!");
									}
								}
								else
								{
									player.sendMessage("Can't find your account!");
								}
							}
							else
							{
								player.sendMessage("You have no pending regions.  Use /Claim help.");
							}
						}
						else if (args[0].equalsIgnoreCase("reject"))
						{
							if (PendingRegions.containsKey(player.getName()))
							{
								player.sendMessage("Removing previous pending region...");
								PendingRegions.remove(player.getName());
								PendingCosts.remove(player.getName());
							}
							else
							{
								player.sendMessage("You have no pending regions.  Use /Claim help.");
							}
						}
						else if (args[0].equalsIgnoreCase("info"))
						{
							RegionManager mgr = WGBukkit.getRegionManager(player.getWorld());
							ApplicableRegionSet app = mgr.getApplicableRegions(player.getLocation().subtract(0, 1, 0));
							String Regions = "";
							
							for (ProtectedRegion r : app)
							{
								Regions += (r.isOwner(player.getName()) ? ChatColor.RED : r.isMember(player.getName()) ? ChatColor.GREEN : ChatColor.WHITE) + r.getId() + " ";
							}
							
							player.sendMessage("The following regions are at your location: " + Regions);
						}
						else if (args[0].equalsIgnoreCase("list"))
						{
							RegionManager mgr = WGBukkit.getRegionManager(player.getWorld());
							TreeMap<String, ProtectedRegion> regs = (TreeMap<String, ProtectedRegion>)mgr.getRegions();		
							String Regions = "";
							
							Iterator<Entry<String, ProtectedRegion>> i = regs.entrySet().iterator();
							while (i.hasNext())
							{
								ProtectedRegion r = i.next().getValue();
								Regions += r.isOwner(player.getName()) ? ChatColor.RED + r.getId() + " " : r.isMember(player.getName()) ? ChatColor.GREEN + r.getId() + " " : "";
							}

							player.sendMessage("You are an " + ChatColor.RED + "Owner " + ChatColor.WHITE + "or " + ChatColor.GREEN + "Member " + ChatColor.WHITE + "of the following regions: " + Regions);
						}
						else
						{
							player.sendMessage("Command not recognized.  Use /Claim help.");							
						}
					}
					else if (args.length == 2)
					{
						if (args[0].equalsIgnoreCase("info"))
						{
							RegionManager mgr = WGBukkit.getRegionManager(player.getWorld());
							TreeMap<String, ProtectedRegion> regs = (TreeMap<String, ProtectedRegion>) mgr.getRegions();
														
							if (regs.containsKey(args[1]))
							{
								ProtectedRegion r = regs.get(args[1]);
								if (r.isMember(player.getName()) || r.isOwner(player.getName()))
								{
									String members = join(r.getMembers().getPlayers().toArray(new String[0]), " ");
								
									player.sendMessage("Information for region: " + r.getId());
									player.sendMessage(String.format("Bounds: (%s, %s, %s) - (%s, %s, %s)",
											r.getMinimumPoint().getBlockX(),
											r.getMinimumPoint().getBlockY(),
											r.getMinimumPoint().getBlockZ(),
											r.getMaximumPoint().getBlockX(),
											r.getMaximumPoint().getBlockY(),
											r.getMaximumPoint().getBlockZ()
											));
									player.sendMessage("Members: " + members);
								}
								else
								{
									player.sendMessage("You do not have access to that region.");
								}
							}	
							else
							{
								player.sendMessage("No region found with the region name: " + args[1]);
							}
						}
						else
						{
							player.sendMessage("Command not recognized.  Use /Claim help.");							
						}
					}
					else if (args.length == 3)
					{
						if (args[0].equalsIgnoreCase("addmember"))
						{
							RegionManager mgr = WGBukkit.getRegionManager(player.getWorld());
							TreeMap<String, ProtectedRegion> regs = (TreeMap<String, ProtectedRegion>) mgr.getRegions();
							
							if (regs.containsKey(args[1]))
							{
								ProtectedRegion r = regs.get(args[1]);
								if (r.isOwner(player.getName()))
								{
									DefaultDomain dd = r.getMembers();
									dd.addPlayer(args[2]);
									r.setMembers(dd);
									
									try {
										mgr.save();
										player.sendMessage(String.format("Added %s to the region %s", args[2], args[1]));
									} catch (ProtectionDatabaseException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
										player.sendMessage("Unknown Error!");
									}
								}
								else
								{
									player.sendMessage("You are not the owner of that region.");
								}
							}	
							else
							{
								player.sendMessage("No region found with the region name: " + args[1]);
							}
						}
						else if (args[0].equalsIgnoreCase("removemember"))
						{
							RegionManager mgr = WGBukkit.getRegionManager(player.getWorld());
							TreeMap<String, ProtectedRegion> regs = (TreeMap<String, ProtectedRegion>) mgr.getRegions();
							
							if (regs.containsKey(args[1]))
							{
								ProtectedRegion r = regs.get(args[1]);
								if (r.isOwner(player.getName()))
								{
									DefaultDomain dd = r.getMembers();
									dd.removePlayer(args[2]);
									r.setMembers(dd);
									
									try {
										mgr.save();
										player.sendMessage(String.format("Removed %s from the region %s", args[2], args[1]));
									} catch (ProtectionDatabaseException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
										player.sendMessage("Unknown Error!");
									}
								}
								else
								{
									player.sendMessage("You are not the owner of that region.");
								}
							}	
							else
							{
								player.sendMessage("No region found with the region name: " + args[1]);
							}							
						}
						else
						{
							int param1 = tryParse(args[0]);
							int param2 = tryParse(args[1]);
							int param3 = tryParse(args[2]);
							
							if (param1 > 10 && param2 > 10 && param3 > 10)
							{
								StartRegion(player, param1, param2, param3);							
							}
							else if (param1>0 && param2>0 && param3>0)
							{
								player.sendMessage("Regions must be a minimum size of 11 in all directions.");
							}
							else
							{
								player.sendMessage("Invalid command, use /Claim help for more information.");								
							}
						}
					}
					else
					{
						player.sendMessage("You have an invalid number of parameters.  Use /Claim help.");
					}
				}
				else
				{
					getLogger().info("ClaimRegion v1.0!  Please use this command as a player.");
				}
			}			
		}
		
		return true;
	}
	
	public void StartRegion(Player player, int X, int Z, int H)
	{		
		X = X / 2;
		Z = Z / 2;
		
		int cost = (X*2+1)*(Z*2+1)*H*5 + 500;
		RegionManager mgr = WGBukkit.getRegionManager(player.getWorld());
		Location l1 = player.getLocation().subtract(X, 1, Z);
		Location l2 = player.getLocation().add(X, H-2, Z);
		BlockVector bv1 = new BlockVector(l1.getBlockX(), l1.getBlockY(), l1.getBlockZ());
		BlockVector bv2 = new BlockVector(l2.getBlockX(), l2.getBlockY(), l2.getBlockZ());
		LocalPlayer lp = WGBukkit.getPlugin().wrapPlayer(player);
		String rgName = player.getName() + Integer.toString(mgr.getRegionCountOfPlayer(lp));
		
		ProtectedRegion reg = new ProtectedCuboidRegion(rgName, bv1, bv2);
		DefaultDomain owners = reg.getOwners();
		owners.addPlayer(player.getName());
		reg.setOwners(owners);
		
		reg.setFlag(DefaultFlag.CHEST_ACCESS, State.DENY);
		reg.setFlag(DefaultFlag.ENTITY_PAINTING_DESTROY, State.DENY);
		reg.setFlag(DefaultFlag.ENTITY_ITEM_FRAME_DESTROY, State.DENY);
		reg.setFlag(DefaultFlag.TNT, State.DENY);
		reg.setFlag(DefaultFlag.OTHER_EXPLOSION, State.DENY);
		reg.setFlag(DefaultFlag.FIRE_SPREAD, State.DENY);
		reg.setFlag(DefaultFlag.DESTROY_VEHICLE, State.DENY);
		reg.setFlag(DefaultFlag.LAVA_FIRE, State.DENY);
		reg.setFlag(DefaultFlag.PLACE_VEHICLE, State.DENY);
		
		HashSet<EntityType> DeniedMobs = new HashSet<EntityType>();
		DeniedMobs.add(EntityType.ZOMBIE);
		DeniedMobs.add(EntityType.SKELETON);
		DeniedMobs.add(EntityType.CREEPER);
		DeniedMobs.add(EntityType.SPIDER);
		DeniedMobs.add(EntityType.SLIME);
		DeniedMobs.add(EntityType.WITCH);
		DeniedMobs.add(EntityType.BAT);
		DeniedMobs.add(EntityType.CAVE_SPIDER);
		DeniedMobs.add(EntityType.ENDERMAN);
		DeniedMobs.add(EntityType.WITHER);
		DeniedMobs.add(EntityType.PIG_ZOMBIE);
		DeniedMobs.add(EntityType.MAGMA_CUBE);
		DeniedMobs.add(EntityType.GHAST);
		DeniedMobs.add(EntityType.BLAZE);
		
		reg.setFlag(DefaultFlag.DENY_SPAWN, (Set<EntityType>)DeniedMobs);
		
		
		if (mgr.getApplicableRegions(reg).size() > 0)
		{
			player.sendMessage("You are overlapping a region, choose a different location.");
		}
		else
		{
			if (PendingRegions.containsKey(player.getName()))
			{
				player.sendMessage("Removing previous pending region...");
				PendingRegions.remove(player.getName());
				PendingCosts.remove(player.getName());
			}
			
			player.sendMessage("Name of the new region: " + rgName);
			player.sendMessage(String.format("Bounds (X, Y, Z) - (X, Y, Z): (%s,  %s, %s) - (%s, %s, %s)", 
					l1.getBlockX(), 
					l1.getBlockY(), 
					l1.getBlockZ(), 
					l2.getBlockX(), 
					l2.getBlockY(), 
					l2.getBlockZ()
					));
			player.sendMessage(String.format("Cost: $%s", cost));
			player.sendMessage("Type /Claim Accept to create your region, or /Claim Reject to remove the cache.");
			
			PendingRegions.put(player.getName(), reg);
			PendingCosts.put(player.getName(), cost);	
		}
	}
	
	public boolean checkRegion(RegionManager mgr, ProtectedRegion rg)
	{
		return mgr.getApplicableRegions(rg).size() > 0;
	}
	
	public int tryParse( String input )  
	{  
		int rtn = -1;
		try  
		{  
			rtn = Integer.parseInt( input );  
		}  
		catch( Exception ex )  
		{      
		} 
		   
	   return rtn;
	}
	
	public static String join(String r[],String d)
	{
	        if (r.length == 0) return "";
	        StringBuilder sb = new StringBuilder();
	        int i;
	        for(i=0;i<r.length-1;i++)
	            sb.append(r[i]+d);
	        return sb.toString()+r[i];
	}
}