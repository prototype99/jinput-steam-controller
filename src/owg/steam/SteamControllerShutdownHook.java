package owg.steam;

import java.lang.ref.WeakReference;

public class SteamControllerShutdownHook extends Thread
{
	private final WeakReference<SteamControllerPlugin> ref;
	
	public SteamControllerShutdownHook(SteamControllerPlugin steamControllerPlugin)
	{
		ref = new WeakReference<SteamControllerPlugin>(steamControllerPlugin);
	}
	
	@Override
	public void run()
	{
		SteamControllerPlugin plugin = ref.get();
		if(plugin != null)
			plugin.shutdown();
	}
}
