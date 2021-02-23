package owg.steam;

public class SteamControllerThread extends Thread
{
	// Notice: This class avoids any references to SteamController or SteamControllerPlugin,
	// so that the controller thread will not keep the plugin alive if the application stops using it.
	
	public final SteamControllerThreadTask[] controllerTasks;
	public volatile boolean alive = true;
	
	public SteamControllerThread(SteamControllerThreadTask[] controllerTasks)
	{
		super("steam-controller-thread");
		this.controllerTasks = controllerTasks;
	}
	
	@Override
	public void run()
	{
		try {
			for(SteamControllerThreadTask ct : controllerTasks)
				ct.init();
			while(alive)
			{
				for(SteamControllerThreadTask ct : controllerTasks)
					ct.run();
			}
		} finally {
			for(SteamControllerThreadTask ct : controllerTasks)
				ct.cleanup();
		}
	}
}
