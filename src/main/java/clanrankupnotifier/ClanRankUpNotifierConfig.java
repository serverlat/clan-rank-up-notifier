package clanrankupnotifier;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("clanrankupnotifier")
public interface ClanRankUpNotifierConfig extends Config
{
    // ClanRankNotifierConfig.java
    @ConfigItem(
            keyName = "eligibleRanks",
            name = "Eligible current ranks",
            description = "Only members whose CURRENT rank is in this list are checked. " +
                    "Use your clan's rank titles or numbers (-1..127) separated by comma ','. " +
                    "Examples:\nRecruit\nCorporal\n20\n100"
    )
    default String eligibleRanks()
    {
        return ""; // empty = no filtering (process all)
    }

    @ConfigItem(
            keyName = "rules",
            name = "Rank rules",
            description = "One rule per line: Days = Rank\n" +
                    "Rank can be a number (-1..127) or YOUR clan's rank title (case-insensitive).\n" +
                    "Examples:\n" +
                    "7=Recruit\n" +
                    "30=Corporal\n" +
                    "60=Sergeant\n" +
                    "Or numeric: 7=1, 30=10, 60=20"
    )
    default String rules()
    {
        return String.join("\n",
                "# Days = RankName or RankNumber",
                "7=Recruit",
                "30=Corporal",
                "60=Sergeant"
        );
    }

    @ConfigItem(
            keyName = "ignorelist",
            name = "Ignored users",
            description = "Ignored users (comma-separated). These names will be skipped."
    )
    default String ignoredUsers()
    {
        return "";
    }

    @ConfigItem(
            keyName = "muteNotifications",
            name = "Mute notifications",
            description = "Check if you'd like to mute notifications about due rank-ups. Set to true by default."
    )
    default boolean muteNotifications() { return true; }
}
