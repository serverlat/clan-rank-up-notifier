package clanrankupnotifier;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ClanRankUpNotifierTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClanRankUpNotifierPlugin.class);
		RuneLite.main(args);
	}
}