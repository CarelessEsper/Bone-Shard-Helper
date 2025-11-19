package com.boneshardhelper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BoneShardHelperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BoneShardHelperPlugin.class);
		RuneLite.main(args);
	}
}