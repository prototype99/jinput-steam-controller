package owg.steam;

import net.java.games.input.Rumbler;
import net.java.games.input.Component.Identifier;

/**Class representing one of the haptic feedback motors of the Steam Controller as a rumbler.*/
public class SCRumbler implements Rumbler
{
	protected final String name;
	protected final Identifier identifier;
	protected final byte rumblerID;
	protected SteamController host;

	public SCRumbler(String name, Identifier identifier, int rumblerID)
	{
		this.name = name;
		this.identifier = identifier;
		this.rumblerID = (byte) rumblerID;
	}

	@Override
	public void rumble(float intensity)
	{
		host.threadTask.rumble(rumblerID, intensity);
	}

	@Override
	public String getAxisName()
	{
		return name;
	}

	@Override
	public Identifier getAxisIdentifier()
	{
		return identifier;
	}

}