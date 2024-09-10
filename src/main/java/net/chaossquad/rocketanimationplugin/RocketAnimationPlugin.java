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
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RocketAnimationPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    private BlockStructure rocketStructure;
    private List<Rocket> rockets;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("rocketanimation").setExecutor(this);
        this.getCommand("rocketanimation").setTabCompleter(this);

        this.rocketStructure = null;
        this.rockets = new ArrayList<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                RocketAnimationPlugin.this.rocketTask();
            }
        }.runTaskTimer(this, 1, 1);
    }

    @Override
    public void onDisable() {

        for (Rocket rocket : this.rockets) {
            rocket.remove();
        }

        this.rockets.clear();

    }

    // UTILITIES

    public void rocketTask() {

        for (Rocket rocket : List.copyOf(this.rockets)) {

            if (rocket.isRemoved()) {
                this.rockets.remove(rocket);
                continue;
            }

            if (rocket.isAnimationEnabled()) {
                rocket.animationTick();
            }

        }

    }

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

                this.rockets.add(new Rocket(WorldUtils.spawnBlockStructure(location.getWorld(), this.rocketStructure, location, List.of(this.getName()))));

                sender.sendMessage("§aRockets spawned");

            }
            case "list" -> {

                sender.sendMessage("§7Current rocket entities:");
                for (int i = 0; i < this.rockets.size(); i++) {
                    Rocket rocket = this.rockets.get(i);
                    sender.sendMessage("§7" + i + ": " + rocket.getDisplays().size() + " entities, " + rocket.getDisplays().stream().filter(blockDisplay -> !blockDisplay.isDead()).count() + " alive");
                }

            }
            case "remove" -> {

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /rocketanimation remove <id>");
                    return true;
                }

                Rocket rocket = this.rockets.get(Integer.parseInt(args[1]));
                rocket.remove();

                sender.sendMessage("§aRocket removed");
            }
            case "clear" -> {

                for (Rocket rocket : this.rockets) {
                    rocket.remove();
                }

                sender.sendMessage("§aCleared all rockets");
            }
            case "reset" -> {

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /rocketanimation reset <id>");
                    return true;
                }

                Rocket rocket = this.rockets.get(Integer.parseInt(args[1]));
                rocket.reset();

                sender.sendMessage("§aRocket reset");

            }
            case "animation" -> {

                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /rocketanimation animation <rocket> disable/enable/status/stop");
                    return true;
                }

                Rocket rocket = this.rockets.get(Integer.parseInt(args[1]));

                switch (args[2]) {
                    case "disable" -> {
                        rocket.setAnimationEnabled(false);
                        sender.sendMessage("§aAnimation disabled");
                    }
                    case "enable" -> {
                        rocket.setAnimationEnabled(true);
                        sender.sendMessage("§aAnimation enabled");
                    }
                    case "status" -> {
                        sender.sendMessage("§7Enabled: " + rocket.isAnimationEnabled());
                        sender.sendMessage("§7Running: " + rocket.isAnimationRunning());
                        sender.sendMessage("§7Parts: " + rocket.getAnimationParts().size());
                        sender.sendMessage("§7Current part: " + rocket.getCurrentAnimationPart());
                        sender.sendMessage("§7Max part time: " + rocket.getMaxAnimationTime());
                        sender.sendMessage("§7Current part time: " + rocket.getCurrentAnimationTime());
                    }
                    case "stop" -> {
                        rocket.stopAnimation();
                        sender.sendMessage("§aAnimation stopped");
                    }
                    default -> sender.sendMessage("§cUnknown subcommand");
                }

            }
            case "flyup" -> {

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /rocketanimation flyup <rocket>");
                    return true;
                }

                Rocket rocket = this.rockets.get(Integer.parseInt(args[1]));

                List<Rocket.Animation> animations = new ArrayList<>();
                int totalDuration = 150;  // Gesamtdauer der Animation (in Ticks)
                float totalDistance = 300.0f;  // Gesamthöhe, die die Rakete erreichen soll (300 Blöcke)
                int steps = 30;  // Anzahl der Schritte in der Animation
                float acceleration = 2.0f;  // Beschleunigung der Rakete

                for (int i = 0; i < steps; i++) {
                    float t = (float) i / (steps - 1);  // Normalisierte Zeit (zwischen 0 und 1)
                    float currentDistance = 0.5f * acceleration * (t * t);  // Quadratische Bewegungsgleichung

                    // Skaliere die aktuelle Entfernung auf die Gesamtdistanz
                    currentDistance *= totalDistance;

                    int duration = totalDuration / steps;  // Dauer jedes Schritts
                    Vector3f transformation = new Vector3f(0, currentDistance, 0);  // Verschiebung in y-Richtung

                    animations.add(new Rocket.Animation(0, duration, transformation));
                }

                rocket.startAnimation(animations);

                sender.sendMessage("§aAnimation flyup started");

            }
            case "flydown" -> {

                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /rocketanimation flyup <rocket>");
                    return true;
                }

                Rocket rocket = this.rockets.get(Integer.parseInt(args[1]));

                List<Rocket.Animation> animations = new ArrayList<>();
                int totalDuration = 150;  // Gesamtdauer der Animation (in Ticks)
                float totalDistance = 300.0f;  // Gesamthöhe, die die Rakete erreichen soll (300 Blöcke)
                int steps = 30;  // Anzahl der Schritte in der Animation
                float acceleration = 2.0f;  // Beschleunigung der Rakete

                for (int i = 0; i < steps; i++) {
                    float t = (float) i / (steps - 1);  // Normalisierte Zeit (zwischen 0 und 1)
                    float currentDistance = 0.5f * acceleration * (t * t);  // Quadratische Bewegungsgleichung

                    // Skaliere die aktuelle Entfernung auf die Gesamtdistanz
                    currentDistance *= totalDistance;

                    // Mache die Entfernung negativ, damit die Rakete nach unten fliegt
                    currentDistance = -currentDistance;

                    int duration = totalDuration / steps;  // Dauer jedes Schritts
                    Vector3f transformation = new Vector3f(0, currentDistance, 0);  // Verschiebung in y-Richtung nach unten

                    animations.add(new Rocket.Animation(0, duration, transformation));
                }

                rocket.startAnimation(animations);

                sender.sendMessage("§aAnimation flydown started");

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
            return List.of("load", "spawn", "list", "remove", "clear", "reset");
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
            case "animation" -> {

                if (args.length == 3) {
                    yield List.of("disable", "enable", "status", "stop");
                } else {
                    yield List.of();
                }

            }
            default -> List.of();
        };
    }

    // GETTER

}
