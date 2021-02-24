package owg.steam;

import org.usb4java.Device;

import static owg.steam.SteamController.*;

public class SteamControllerDevice
{
	// Notice: This class avoids any references to SteamController or SteamControllerPlugin,
	// so that the controller thread will not keep the plugin alive if the application stops using it.
	
	public final Device device;
	public final short pid;
	public final int portNo;

	public final byte endpoint;
	public final short controlIndex;
	public final int interfaceNo;
	
	public SteamControllerDevice(Device device, short pid, int portNo, byte endpoint, short controlIndex, int interfaceNo)
	{
		this.device = device;
		this.pid = pid;
		this.portNo = portNo;
		
		this.endpoint = endpoint;
		this.controlIndex = controlIndex;
		this.interfaceNo = interfaceNo;
	}


	public boolean isWired()
	{
		return pid == PID_WIRED;
	}
	public boolean isWireless()
	{
		return pid == PID_WIRELESS;
	}
}
