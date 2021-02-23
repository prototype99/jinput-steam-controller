package owg.steam;

import java.io.IOException;

import net.java.games.input.AbstractComponent;
import net.java.games.input.Event;

/**Base class for Steam Controller component implementations (buttons and axes).*/
public abstract class SCComponent extends AbstractComponent
{
	// Notice: This class avoids any references to SteamController or SteamControllerPlugin,
	// so that the controller thread will not keep the plugin alive if the application stops using it.
	
	public final boolean relative;
	protected SteamControllerData data = null;
	protected SteamControllerConfig config = null;
	/**The poll value after the last call to {@link SteamController#getNextDeviceEvent(Event)}*/
	protected float cachedValue = 0.0f;

	protected SCComponent(String name, Identifier id, boolean relative)
	{
		super(name, id);
		this.relative = relative;
	}
	@Override
	public boolean isRelative()
	{
		return relative;
	}

	@Override
	protected final float poll() throws IOException {
		return pollFrom(data.lPadData, data.lStickData, data.latestData);
	}
	
	public abstract float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData);
}