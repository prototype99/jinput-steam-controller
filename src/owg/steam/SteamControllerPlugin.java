package owg.steam;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class SteamControllerPlugin extends ControllerEnvironment
{
	public static void main(String[] args)
	{
		//Run this for a primitive testing environment
		SteamController.properties = new Properties();
		FileReader reader = null;
		try {
			reader = new FileReader("sc.properties");
			SteamController.properties.load(reader);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null)
			{
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		SteamControllerPlugin p = new SteamControllerPlugin();
		SteamController[] controllers = (SteamController[]) p.getControllers();
		if(controllers.length == 0)
		{
			System.err.println("No steam controllers found");
			return;
		}
		System.out.println("Found controllers: "+Arrays.toString(controllers));

	    while(true)
	    {
	    	if(!testInput(controllers))
	    		break;
	    }
	}

	private static boolean testInput(SteamController[] controllers) {
	    int i = 0;
		if(controllers[i].getComponent(Identifier.Button.A).getPollData() > 0.5f)
    		controllers[i].getRumblers()[0].rumble(0.01f);
    	if(controllers[i].getComponent(Identifier.Button.B).getPollData() > 0.5f)
    		controllers[i].getRumblers()[1].rumble(0.5f);
    	boolean r = controllers[i].poll();
    	
    	if(controllers[i].getComponent(Identifier.Button._19).getPollData() > 0.5)
    		System.out.println("LP");
    	if(controllers[i].getComponent(Identifier.Button._20).getPollData() > 0.5)
    		System.out.println("RP");
    	
    	float rx = controllers[i].getComponent(Identifier.Axis.RX).getPollData();
    	float ry = controllers[i].getComponent(Identifier.Axis.RY).getPollData();
    	float rz = controllers[i].getComponent(Identifier.Axis.RZ).getPollData();
    	if(Math.abs(rx) > 0.1)
    		System.out.println("RX="+rx);
    	if(Math.abs(ry) > 0.1)
    		System.out.println("RY="+ry);
    	if(Math.abs(rz) > 0.1)
    		System.out.println("RZ="+rz);
    	if(controllers[i].getComponent(Identifier.Button.SELECT).getPollData() >= 0.5)
    	{
    		System.out.println("left menu button pressed");
    		return false;
    	}
    	
    	if(!r)
    		return false;
    	try
		{
			Thread.sleep(33);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
    	return true;
	}

	protected Context context;
	protected final Thread shutdownHook;
	
	protected SteamController[] controllers;
	
	protected final Object lock = new Object();
	
	public SteamControllerPlugin()
	{
		context = new Context();
		{
			int result = LibUsb.init(context);
			if (result != LibUsb.SUCCESS) 
				throw new LibUsbException("Unable to initialize libusb.", result);
		}
		//Add shutdown hook.
		shutdownHook = new Thread() {
			@Override
			public void run()
			{
				synchronized (lock)
				{
					System.out.println("Info: Steam Controller plugin closing");
					if(controllers != null)
					{
						for(SteamController c : controllers)
							c.close();
						controllers = null;
					}
					if(context != null)
					{
						LibUsb.exit(context);
						context = null;
					}
				}
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
		{
		    // Read the USB device list
		    DeviceList list = new DeviceList();
		    {
			    int result = LibUsb.getDeviceList(context, list);
			    if (result < 0) 
			    	throw new LibUsbException("Unable to get device list", result);
		    }
		    
		    ArrayList<SteamController> cList = new ArrayList<SteamController>();
	
	        // Iterate over all devices and scan for the right one
	        for (Device device: list)
	        {
	        	try 
	        	{
		            DeviceDescriptor descriptor = new DeviceDescriptor();
		            {
		            int result = LibUsb.getDeviceDescriptor(device, descriptor);
		            if (result != LibUsb.SUCCESS)
		            	throw new LibUsbException("Unable to read device descriptor", result);
		            }
					short vid = descriptor.idVendor();
					short pid = descriptor.idProduct();
					if(vid == 0x28DE && (pid == SteamController.PID_WIRELESS || pid == SteamController.PID_WIRED))
					{
						if(pid == SteamController.PID_WIRED)
							cList.add(new SteamController(this, device, pid, 2, 3));
						else
						{
							for(int i = 0; i<4; i++)
							{
								cList.add(new SteamController(this, device, pid, 1+i, 2+i));
							}
						}
					}
	        	} catch(Exception err) {
	        		System.out.println("Info: Failed to initialize Steam Controller");
	        		err.printStackTrace(System.out);
	        	}
	        }
	        // Ensure the allocated device list is freed
	        LibUsb.freeDeviceList(list, true);
		    controllers = cList.toArray(new SteamController[cList.size()]);
		}
	}
	
	@Override
	protected void finalize()
	{
		synchronized (lock)
		{
			shutdownHook.run();
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
		}
	}

	@Override
	public Controller[] getControllers()
	{
		return controllers;
	}

	@Override
	public boolean isSupported()
	{
		return true;
	}
}
