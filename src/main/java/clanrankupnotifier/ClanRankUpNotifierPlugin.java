package clanrankupnotifier;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.*;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

@PluginDescriptor(
        name = "Clan Rank Up Notifier",
        description = "Notifies when clan members hit time-in-clan thresholds (configurable)",
        tags = {"clan", "rank", "notify"}
)
public class ClanRankUpNotifierPlugin extends Plugin
{
    private static final Logger LOG = LoggerFactory.getLogger(ClanRankUpNotifierPlugin.class);

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private Notifier notifier;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ClanRankUpNotifierConfig config;
    @Inject private ScheduledExecutorService scheduler;
    @Inject private ConfigManager configManager;

    private ClanRankUpNotifierPanel panel;
    private NavigationButton navButton;

    private ScheduledFuture<?> task;

    private volatile NavigableMap<Integer, String> rules = new TreeMap<>();
    private final Map<String, String> notifiedToday = new HashMap<>();
    private volatile Set<String> eligibleRanksSet = Set.of();
    private volatile Set<String> ignoredUsersSet   = Set.of();
    private volatile boolean muteNotifications = true;
    private LocalDate lastNotificationDate = null;

    @Provides
    ClanRankUpNotifierConfig provideConfig(ConfigManager cm) { return cm.getConfig(ClanRankUpNotifierConfig.class); }

    @Override
    protected void startUp()
    {
        panel = new ClanRankUpNotifierPanel(this::runManualCheck, this::ignoreUserFromPanel);

        BufferedImage icon = null;
        try { icon = ImageUtil.loadImageResource(ClanRankUpNotifierPlugin.class, "icon.png"); }
        catch (Exception ignored) {}
        if (icon == null) icon = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        navButton = NavigationButton.builder()
                .tooltip("Clan Rank Notifier")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        parseRules();
        clientThread.invokeLater(this::checkRosterAndNotify);
        LOG.info("[ClanRankNotifier] started");
    }

    @Override
    protected void shutDown()
    {
        if (task != null) { task.cancel(true); task = null; }
        if (navButton != null) { clientToolbar.removeNavigation(navButton); navButton = null; }
        panel = null;
        LOG.info("[ClanRankNotifier] stopped");
    }

    private void parseRules()
    {
        TreeMap<Integer, String> tmp = new TreeMap<>();
        for (String line : config.rules().split("\\R"))
        {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            String[] parts = s.split("=");
            if (parts.length != 2) continue;

            try {
                int days = Integer.parseInt(parts[0].trim());
                String rank = parts[1].trim();
                tmp.put(days, rank);
            } catch (NumberFormatException ignored) { }
        }
        rules = tmp;

        eligibleRanksSet = csvToLowerSet(config.eligibleRanks());
        ignoredUsersSet  = csvToLowerSet(config.ignoredUsers());
        muteNotifications = config.muteNotifications();
    }

    private void ignoreUserFromPanel(String name)
    {
        if (name == null || name.isBlank()) return;

        String n = name.trim();

        clientThread.invokeLater(() ->
        {
            String current = config.ignoredUsers();
            Set<String> set = new java.util.LinkedHashSet<>(); // stable order

            if (current != null && !current.isBlank())
            {
                for (String p : current.split(","))
                {
                    String t = p.trim();
                    if (!t.isEmpty()) set.add(t);
                }
            }

            boolean exists = set.stream().anyMatch(x -> x.equalsIgnoreCase(n));
            if (!exists) set.add(n);

            String updated = String.join(", ", set);

            configManager.setConfiguration("clanrankupnotifier", "ignorelist", updated);

            parseRules();
            checkRosterAndNotify();
        });
    }


    private static Set<String> csvToLowerSet(String s) {
        if (s == null) return Set.of();
        String[] parts = s.split(",");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
               String t = p.trim().toLowerCase();
                if (!t.isEmpty()) out.add(t);
            }
        return out;
    }

    private ClanSettings getClan()
    {
        try {
            ClanSettings cs = client.getClanSettings(ClanID.CLAN);
            if (cs != null && cs.getMembers() != null) return cs;
        } catch (Throwable ignored) { }

        try {
            ClanSettings cs = client.getClanSettings();
            if (cs != null && cs.getMembers() != null) return cs;
        } catch (Throwable ignored) { }

        return null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        if (e.getGameState() == GameState.LOGGED_IN)
            clientThread.invokeLater(this::checkRosterAndNotify);
    }

    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged e)
    {
        clientThread.invokeLater(this::checkRosterAndNotify);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"clanrankupnotifier".equals(e.getGroup())) return;
        parseRules();
        clientThread.invokeLater(this::checkRosterAndNotify);
    }

    private void checkRosterAndNotify()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return;

        ClanSettings cs = getClan();
        if (cs == null || rules.isEmpty())
        {
            panel.setRows(List.of());
            return;
        }
        else
        {
            panel.setInfoText("");
        }

        LocalDate today = LocalDate.now();
        if (!today.equals(lastNotificationDate))
        {
            notifiedToday.clear();
            lastNotificationDate = today;
        }

        final List<String> due = new ArrayList<>();

        for (ClanMember member : cs.getMembers())
        {
            final String name = member.getName();
            if (name == null || name.isBlank()) continue;

            final LocalDate joined = getJoinDate(member);
            if (joined == null) continue;

            final long days = ChronoUnit.DAYS.between(joined, today);
            final Map.Entry<Integer, String> rule = rules.floorEntry((int) days);
            if (rule == null) continue;

            final String targetRankName = rule.getValue();
            final ClanRank currentRank = member.getRank();
            final ClanTitle currentClanRankTitle = cs.titleForRank(currentRank);
            final boolean noRank = currentClanRankTitle == null;
            final String currentRankName = noRank ? "Not ranked" : cs.titleForRank(currentRank).getName();

            final boolean eligibleRank = eligibleRanksSet.contains(currentRankName.toLowerCase());
            final boolean correctRank = targetRankName.trim().equalsIgnoreCase(currentRankName.trim());
            final boolean inIgnoreList = ignoredUsersSet.contains(name.toLowerCase());

            if (!eligibleRank || correctRank || noRank || inIgnoreList) continue;

            due.add(String.format("%s,%d,%s,%s", name, days, targetRankName, currentRankName));
            final String last = notifiedToday.get(name);
            if (!Objects.equals(last, targetRankName) && !muteNotifications) {
                notifier.notify(String.format(
                        "[Clan Rank] %s is %d days in clan → due for %s (current: %s)",
                        name, days, targetRankName, currentRankName));
                notifiedToday.put(name, targetRankName);
            }
        }

        due.sort((a, b) -> {
                        int da = Integer.parseInt(a.split(",")[1].trim());
                        int db = Integer.parseInt(b.split(",")[1].trim());
                        return Integer.compare(db, da);
                    });

        panel.setInfoText("Members due: " + due.size());
        panel.setRows(due);
    }

    private void runManualCheck()
    {
        clientThread.invoke(() -> {
            try {
                panel.setBusy(true);
                checkRosterAndNotify();
            } finally {
                panel.setBusy(false);
            }
        });
    }

    private LocalDate getJoinDate(ClanMember member)
    {
        try {
            LocalDate date = member.getJoinDate();
            if (date != null) return date;
        } catch (Throwable ignored) { }

        return null;
    }
}
