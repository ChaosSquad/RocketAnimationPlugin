package net.chaossquad.rocketanimationplugin;

import net.chaossquad.mclib.WorldUtils;
import net.chaossquad.mclib.blocks.BlockBox;
import net.chaossquad.mclib.blocks.BlockStructure;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RocketAnimationPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    private BlockStructure rocketStructure;
    private List<List<BlockDisplay>> rockets;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("rocketanimation").setExecutor(this);
        this.getCommand("rocketanimation").setTabCompleter(this);

        this.rocketStructure = null;
        this.rockets = new ArrayList<>();
    }

    @Override
    public void onDisable() {

        for (List<BlockDisplay> rockets : this.rockets) {
            for (BlockDisplay display : rockets) {
                display.remove();
            }
        }

        this.rockets.clear();

    }

    // UTILITIES

    // LISTENER

    // COMMAND

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (sender != this.getServer().getConsoleSender() && !sender.hasPermission("rocketanimation")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /rocketanimation [command]");
            return true;
        }

        switch (args[0]) {
            case "load" -> {

                if (args.length < 7) {
                    sender.sendMessage("§cUsage: /rocketanimation load <x1> <y1> <z1> <x2> <y2> <z2> [world]");
                    return true;
                }

                BlockBox box = new BlockBox(
                        Integer.parseInt(args[1]),
                        Integer.parseInt(args[2]),
                        Integer.parseInt(args[3]),
                        Integer.parseInt(args[4]),
                        Integer.parseInt(args[5]),
                        Integer.parseInt(args[6])
                );
                box.sort();

                World world = args.length > 7 ? this.getServer().getWorld(args[7]) : null;
                if (world == null && sender instanceof Player player) world = player.getWorld();
                if (world == null) {
                    sender.sendMessage("§cWorld not found!");
                    return true;
                }

                BlockStructure structure = new BlockStructure(box);
                structure.copy(box.getMin().toLocation(world));
                this.rocketStructure = structure;

                sender.sendMessage("§aStructure loaded!");
            }
            case "spawn" -> {

                if (this.rocketStructure == null) {
                    sender.sendMessage("§cNo structure loaded!");
                    return true;
                }

                // Location

                Location location = null;
                if (args.length > 4) {
                    location = new Location(
                            this.getServer().getWorld(args[1]),
                            Integer.parseInt(args[2]),
                            Integer.parseInt(args[3]),
                            Integer.parseInt(args[4])
                    );
                }
                if (location == null && sender instanceof Player player) {
                    location = player.getLocation().clone();
                }
                if (location == null) {
                    sender.sendMessage("§cLocation not specified!");
                    return true;
                }

                // Spawn

                List<BlockDisplay> spawned = WorldUtils.spawnBlockStructure(location.getWorld(), this.rocketStructure, location, List.of(this.getName()));
                this.rockets.add(spawned);

                sender.sendMessage("§aRockets spawned");

            }
            case "list" -> {

                sender.sendMessage("§7Current rocket entities:");
                for (int i = 0; i < this.rockets.size(); i++) {
                    List<BlockDisplay> rocket = this.rockets.get(i);
                    sender.sendMessage("§7" + i + ": " + rocket.size() + " entities, " + rocket.stream().filter(blockDisplay -> !blockDisplay.isDead()).count() + " alive");
                }

            }
            case "remove" -> {

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /rocketanimation remove <id>");
                    return true;
                }

                List<BlockDisplay> rocket = this.rockets.get(Integer.parseInt(args[1]));
                for (BlockDisplay entity : rocket) {
                    entity.remove();
                }

                this.rockets.remove(rocket);
                sender.sendMessage("§aRocket removed");
            }
            case "clear" -> {

                for (List<BlockDisplay> rocket : this.rockets) {
                    for (BlockDisplay entity : rocket) {
                        entity.remove();
                    }
                }

                this.rockets.clear();
                sender.sendMessage("§aCleared all rockets");
            }
            case "reset" -> {

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /rocketanimation reset <id>");
                    return true;
                }

                List<BlockDisplay> rocket = this.rockets.get(Integer.parseInt(args[1]));
                for (BlockDisplay entity : rocket) {
                    entity.setInterpolationDuration(-1);
                    entity.setInterpolationDelay(-1);
                    entity.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1), new Quaternionf()));
                }

                sender.sendMessage("§aRocket reset");

            }
            case "up" -> {

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /rocketanimation up <id>");
                    return true;
                }

                List<BlockDisplay> rocket = this.rockets.get(Integer.parseInt(args[1]));
                for (BlockDisplay entity : rocket) {
                    entity.setInterpolationDuration(100);
                    entity.setInterpolationDelay(-1);
                    entity.setTransformation(new Transformation(new Vector3f(0, 10, 0), new Quaternionf(), new Vector3f(1), new Quaternionf()));
                }

                sender.sendMessage("§aRocket moved up");

            }
            default -> sender.sendMessage("§cUnknown subcommand");
        }

        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length < 1) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("load", "spawn", "list", "remove", "clear");
        }

        return switch (args[0]) {
            case "load" -> {

                if (!(sender instanceof Player player)) yield List.of();

                Location lookingAt = player.getTargetBlock(null, 4).getLocation().clone();
                int value;
                switch (args.length) {
                    case 2, 5 -> value = lookingAt.getBlockX();
                    case 3, 6 -> value = lookingAt.getBlockY();
                    case 4, 7 -> value = lookingAt.getBlockZ();
                    default -> {
                        yield List.of();
                    }
                }

                yield List.of(String.valueOf(value));
            }
            default -> List.of();
        };
    }

    // GETTER

}
