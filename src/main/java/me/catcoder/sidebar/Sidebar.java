package me.catcoder.sidebar;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class Sidebar {

    private final Set<UUID> viewers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<SidebarLine> lines = new ArrayList<>();
    private final ScoreboardObjective objective;

    /**
     * Construct a new sidebar instance.
     *
     * @param objective a name of scoreboard objective
     * @param title     a title of sidebar
     */
    public Sidebar(@NonNull String objective, @NonNull String title) {
        this.objective = new ScoreboardObjective(objective, title);
    }

    /**
     * Update the title of the sidebar.
     *
     * @param title title to be updated
     */
    public void setTitle(@NonNull String title) {
        objective.setDisplayName(title);
        broadcast(objective::updateValue);
    }

    /**
     * Updates the index of the line shifting it by an offset.
     *
     * @param line   the line
     * @param offset the offset
     */
    public void shiftLine(SidebarLine line, int offset) {
        lines.remove(line);
        lines.add(offset, line);

        updateAllLines(); // recalculate indices
    }

    /**
     * Schedules the task to update all dynamic lines at fixed rate.
     *
     * @param delay  delay in ticks
     * @param period period in ticks
     * @param plugin target plugin
     * @return the scheduled task
     */
    public BukkitTask updatePeriodically(long delay, long period, @NonNull Plugin plugin) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateAllLines, delay, period);
    }

    public SidebarLine addLine(@NonNull String text) {
        return addLine(x -> text, true);
    }

    public SidebarLine addBlankLine() {
        return addLine("");
    }

    public SidebarLine addDynamicLine(@NonNull Function<Player, String> updater) {
        return addLine(updater, false);
    }

    public SidebarLine addStaticLine(@NonNull Function<Player, String> updater) {
        return addLine(updater, true);
    }

    private SidebarLine addLine(@NonNull Function<Player, String> updater, boolean staticText) {
        SidebarLine line = new SidebarLine(updater, objective.getName() + lines.size(), staticText, lines.size());
        lines.add(line);
        return line;
    }

    /**
     * Removes line from sidebar.
     *
     * @param line the line
     */
    public void removeLine(@NonNull SidebarLine line) {
        if (lines.remove(line) && line.getScore() != -1) {
            broadcast(p -> line.removeTeam(p, objective.getName()));
            updateAllLines();
        }
    }

    public Optional<SidebarLine> maxLine() {
        return lines.stream()
                .filter(line -> line.getScore() != -1)
                .max(Comparator.comparingInt(SidebarLine::getScore));
    }

    public Optional<SidebarLine> minLine() {
        return lines.stream()
                .filter(line -> line.getScore() != -1)
                .min(Comparator.comparingInt(SidebarLine::getScore));
    }

    /**
     * Update the single line.
     *
     * @param line target line.
     */
    public void updateLine(@NonNull SidebarLine line) {
        if (lines.contains(line)) {
            broadcast(p -> line.updateTeam(p, line.getScore(), objective.getName()));
        }
    }

    /**
     * Update all dynamic lines of the sidebar.
     */
    public void updateAllLines() {
        int index = lines.size();

        for (SidebarLine line : lines) {
            // if line is not created yet
            if (line.getScore() == -1) {
                line.setScore(index--);
                broadcast(p -> line.createTeam(p, objective.getName()));
                continue;
            }

            int prevIndex = line.getScore();
            line.setScore(index--);

            broadcast(p -> line.updateTeam(p, prevIndex, objective.getName()));
        }
    }

    /**
     * Remove all viewers currently receiving this sidebar.
     */
    public void removeViewers() {
        for (UUID id : viewers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                removeViewer(player);
            }
        }
    }

    /**
     * Sends this sidebar with all lines to the player.
     *
     * @param player target player
     */
    public void addViewer(@NonNull Player player) {
        if (viewers.add(player.getUniqueId())) {
            updateAllLines();

            objective.create(player);
            lines.forEach(line -> line.createTeam(player, objective.getName()));
            objective.display(player);
        }
    }

    /**
     * Removes sidebar for the target player.
     *
     * @param player target player
     */
    public void removeViewer(@NonNull Player player) {
        if (viewers.remove(player.getUniqueId())) {
            updateAllLines();

            lines.forEach(line -> line.removeTeam(player, objective.getName()));
            objective.remove(player);
        }
    }

    public Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    public List<SidebarLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public ScoreboardObjective getObjective() {
        return objective;
    }

    private void broadcast(@NonNull Consumer<Player> consumer) {
        viewers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);

        for (UUID id : viewers) {
            // double check
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }

            consumer.accept(player);
        }
    }
}
