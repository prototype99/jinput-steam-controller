package owg.steam;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import javax.swing.Timer;

import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import net.java.games.input.Component.Identifier;

/**Thread-task that performs Steam Controller USB I/O. This is necessary, 
 * particularly on Linux, because synchronous transfer with libusb is not very good.
 * This thread also handles the auto haptic feedback computation, if enabled.*/
public class SteamControllerThreadTask 
{
	// Notice: This class avoids any references to SteamController or SteamControllerPlugin,
	// so that the controller thread will not keep the plugin alive if the application stops using it.
	
	protected static final Robot robot;
	static 
	{
		Robot r = null;
		try
		{
			r = new Robot();
		} catch (AWTException e)
		{
			r = null;
			e.printStackTrace();
		}
		robot = r;
	}
	
	public final SteamControllerConfig config;
	public final SteamControllerDevice device;
	
	protected IOException fault = null;
	protected final Object lock = new Object();

	protected DeviceHandle handle;
	protected boolean kernelDriver;
	protected boolean interfaceClaimed;
	protected boolean connected;

	/**Direct buffer used for USB operations*/
	protected final ByteBuffer data = ByteBuffer.allocateDirect(64);
	/**Direct buffer used for USB operations*/
	protected final IntBuffer transferred = ByteBuffer.allocateDirect(4).asIntBuffer();

	protected SCComponent[] components;
	protected SCButton[] clickQueue = new SCButton[32];//Default event queue size from AbstractController
	protected int clickQueueHead = 0, clickQueueTail = 0;

	/**Data specific to left pad*/
	protected final byte[] lPadData = new byte[64];
	/**Data specific to left stick*/
	protected final byte[] lStickData = new byte[64];
	/**Specifies whether the latest data is found in lPadDataServer (true) or lStickDataServer (false).*/
	protected boolean lPadIsLatestData = true;
	protected long lastUpdateTimeNanos = Long.MIN_VALUE;

	protected final SCComponent lpx, lpy, rpx, rpy, grz, grx;
	protected float lx=0, ly=0, rx=0, ry=0, gz=0, gx=0;
	protected long padTime = Long.MIN_VALUE;
	protected final int[] hapticOutcodes = {-1, -1};

	protected final Point mouseOffset = new Point();	
	protected final Timer mouseUpdater;

	protected int[] hapticIntensity = {0x0000, 0x0000};
	protected int[] hapticPeriod = {0x0000, 0x0000};
	protected int[] hapticCount = {0x0000, 0x0000};
	protected float[] vibration = {0.0f, 0.0f};
	protected long[] vibrationTimes = {Long.MIN_VALUE, Long.MIN_VALUE};

	public SteamControllerThreadTask(SteamController controller) throws LibUsbException 
	{
		components = (SCComponent[]) controller.getComponents();
		
		this.config = controller.config;
		this.device = controller.device;
		//Note: Failed handle open needs no cleanup
		handle = new DeviceHandle();
		{
			int result = LibUsb.open(device.device, handle);
			if (result != LibUsb.SUCCESS) {
				handle = null;
				throw new LibUsbException("Unable to open USB device", result);
			}
		}

		try {
			// Check if kernel driver must be detached
			boolean detach =  LibUsb.kernelDriverActive(handle, device.interfaceNo) == 1;
			//Note: It is recommended to check the value of
			//LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER),
			//but this always returns false (even if the function works)

			// Detach the kernel driver
			if (detach)
			{
				int result = LibUsb.detachKernelDriver(handle,  device.interfaceNo);
				if (result != LibUsb.SUCCESS) 
					throw new LibUsbException("Unable to detach kernel driver", result);
			}
			kernelDriver = detach;
			
			{
				int result = LibUsb.claimInterface(handle, device.interfaceNo);
				if (result != LibUsb.SUCCESS) 
					throw new LibUsbException("Unable to claim interface", result);
			}
			interfaceClaimed = true;
		}
		catch(LibUsbException e)
		{
			cleanup();
			throw e;
		}

		
		lpx = ((SCComponent)controller.getComponent(Identifier.Axis.X_FORCE));
		lpy = ((SCComponent)controller.getComponent(Identifier.Axis.Y_FORCE));
		rpx = ((SCComponent)controller.getComponent(Identifier.Axis.RX_FORCE));
		rpy = ((SCComponent)controller.getComponent(Identifier.Axis.RY_FORCE));
		grz = ((SCComponent)controller.getComponent(Identifier.Axis.RZ));
		grx = ((SCComponent)controller.getComponent(Identifier.Axis.RX));
		
		mouseUpdater = new Timer(16, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PointerInfo ptr = MouseInfo.getPointerInfo();
				if(ptr != null)
				{
					Point mouse = ptr.getLocation();
					synchronized (mouseOffset)
					{
						robot.mouseMove(mouse.x+mouseOffset.x, mouse.y-mouseOffset.y);
						mouseOffset.x = 0;
						mouseOffset.y = 0;
					}
				}
			}
		});
	}
	public void init() {
		try {
			connected = device.isWired();
			if(connected)
				doSetup();
			else
				doRequestCommStatus();
		} catch(Exception err) {
			System.out.println("Info: "+this+" failed to initialize ("+err.toString()+")");
			synchronized (lock) {
				if(err instanceof IOException)
					fault = (IOException) err;
				else
					fault = new IOException(err);
			}
		}
	}

	public boolean run() {
		if(fault != null)
			return false;
		
		try {
			if(doInterruptTransfer(4L))
			{
				int bytes = transferred.get(0);
				if(bytes == 64)
				{
					if(data.get(2) == SteamController.EV_INPUT_DATA)
					{
						if(connected)
							processInputData();
						//Note: Events received before wireless connect are not processed,
						//because they are residue events with outdated state
					}
					else if(data.get(2) == SteamController.EV_WIRELESS_CONNECT)
					{
						//data[3] is 1, because size is 1 byte
						if(!connected && data.get(4) == SteamController.STEAM_WIRELESS_CONNECT)
						{
							System.out.println("Info: "+this+" connected");
							connected = true;
							doSetup();//Need to (re)apply config here
						}
						else if (connected && data.get(4) == SteamController.STEAM_WIRELESS_DISCONNECT)
						{
							System.out.println("Info: "+this+" disconnected");
							connected = false;
							zero();
						}
					}
					else if(data.get(2) == SteamController.EV_BATTERY_STATUS)
					{
						if(!connected)
						{
							//linux/drivers/hid/hid-steam does this, not sure if necessary
							System.out.println("Info: "+this+" connected via battery status");
							connected = true;
							doSetup();
						}
					}
					//else: unknown event
				}
				else
					System.out.println("Info: Unusual transfer length: "+transferred.get(0));
			}
			else
			{
				//(Timeout is a regular occurrence with wireless controllers)
			}

		} catch(Exception err) {
			System.out.println("Info: "+this+" disconnected irregularly ("+err.toString()+")");
			synchronized (lock) {
				if(err instanceof IOException)
					fault = (IOException) err;
				else
					fault = new IOException(err);
			}
		}
		return connected;
	}

	private void doRequestCommStatus() {
		try {
			data.put( 0, SteamController.STEAM_CMD_REQUEST_CONNECTION_STATUS);
			doControlTransfer(250L);
		} catch(IOException err) {
			System.out.println("Info: Failed to request steam controller connection status:");
			System.out.println(err.toString());
		}
	}

	private void zero() {
		lastUpdateTimeNanos = System.nanoTime();
		for(SCComponent c : components)
		{
			if(c instanceof SCButton)
			{
				SCButton b = ((SCButton)c);
				if(b.latestValue != 0)
				{
					addToClickQueue(b);
					b.latestValue = 0;
				}
			}
		}
		Arrays.fill(lPadData, (byte)0);
		Arrays.fill(lStickData, (byte)0);
		lPadIsLatestData = true;



		padTime = lastUpdateTimeNanos;
		Arrays.fill(hapticOutcodes, -1);
		lx=0; 
		ly=0; 
		rx=0; 
		ry=0;
	}

	private void processInputData() {
		synchronized (lock) {
			lPadIsLatestData = (data.get(10)&8) != 0;
			byte[] dst = lPadIsLatestData?lPadData:lStickData;
			fetchData(dst);

			if((data.get(10)&128) == 0)
			{
				//lpad and stick are not used in conjunction
				if(lPadIsLatestData)
				{
					//Stick is not currently in use
					lStickData[16] = 0;
					lStickData[17] = 0;
					lStickData[18] = 0;
					lStickData[19] = 0;
				}
				else
				{
					//lpad is not currently in use
					lPadData[10] &= ~(2|8);
					lPadData[16] = 0;
					lPadData[17] = 0;
					lPadData[18] = 0;
					lPadData[19] = 0;
				}
			}
			for(SCComponent c : (SCComponent[])components)
			{
				if(c instanceof SCButton)
				{
					SCButton b = ((SCButton)c);
					float pv = b.pollFrom(lPadData, lStickData, dst);
					if(pv != b.latestValue)
					{
						addToClickQueue(b);
						b.latestValue = pv;
					}
				}
			}
			
			if((config.gyroMouseX != 0 || config.gyroMouseY != 0) && (config.applyConfiguration && (config.gyroMode&SteamController.STEAM_GYRO_MODE_SEND_RAW_GYRO) != 0))
			{
				gz += (config.gyroMouseX*grz.pollFrom(lPadData, lStickData, dst)*1000L);
				gx += (config.gyroMouseY*grx.pollFrom(lPadData, lStickData, dst)*1000L);
				int buttons = (dst[8]&0xFF) | ((dst[9]&0xFF)<<8) | ((dst[10]&0xFF)<<16) | ((dst[11]&0xFF)<<24);
				if((config.gyroMouseEnableMask == 0 || (buttons&config.gyroMouseEnableMask) != 0) &&
						(buttons&config.gyroMouseDisableMask) == 0 && robot != null)
				{
					synchronized (mouseOffset)
					{
						if(!mouseUpdater.isRunning())
							mouseUpdater.start();
						mouseOffset.x += (int)gz;
						mouseOffset.y += (int)gx;
					}
				}
				gz = gz%1.0f;
				gx = gx%1.0f;
			}	
			
			lastUpdateTimeNanos = System.nanoTime();

			if(lastUpdateTimeNanos > padTime+(long)33E6)
			{
				if(config.leftPadAutoHaptics)
				{
					float lx0 = lpx.pollFrom(lPadData, lStickData, dst);
					float ly0 = lpy.pollFrom(lPadData, lStickData, dst);
					computeAutoHaptics(SteamController.STEAM_RUMBLER_LEFT, 
							lx, ly, lx0, ly0, lastUpdateTimeNanos-padTime);
					lx = lx0;
					ly = ly0;
				}
				else
					zeroHaptics(SteamController.STEAM_RUMBLER_LEFT);
				doVibration(SteamController.STEAM_RUMBLER_LEFT);

				if(config.rightPadAutoHaptics)
				{
					float rx1 = rpx.pollFrom(lPadData, lStickData, dst);
					float ry1 = rpy.pollFrom(lPadData, lStickData, dst);
					computeAutoHaptics(SteamController.STEAM_RUMBLER_RIGHT, 
							rx, ry, rx1, ry1, lastUpdateTimeNanos-padTime);
					rx = rx1;
					ry = ry1;
				}
				else
					zeroHaptics(SteamController.STEAM_RUMBLER_RIGHT);
				doVibration(SteamController.STEAM_RUMBLER_RIGHT);
				padTime = lastUpdateTimeNanos;
			}
		}
	}

	private void doVibration(byte rumblerID) {
		int vibe;
		if(vibrationTimes[rumblerID] < lastUpdateTimeNanos-(long)200E6)
			vibe = 0;
		else
			vibe = (int) (0xFFFF * 
					vibration[rumblerID] * 
					(1.0 - ((lastUpdateTimeNanos-vibrationTimes[rumblerID])/200L)/1E6));
		if(vibe > hapticIntensity[rumblerID])
			doForceFeedback(rumblerID, vibe, 0xB000, 0x0002);
		else
			doForceFeedback(rumblerID, hapticIntensity[rumblerID], hapticPeriod[rumblerID], hapticCount[rumblerID]);
		/*vibrationTimes[rumblerID] = (int)(intensity*65535);
		hapticPeriod[rumblerID] = 0xB000;
		hapticCount[rumblerID] = 0x0040;*/
	}

	protected void computeAutoHaptics(byte rumblerID, float x0, float y0, float x1, float y1, long dt)
	{
		float snap = 0;
		int snoop = 0;
		double d1 = Math.min(1.0f, Math.sqrt(Math.pow(x1, 2)+Math.pow(y1, 2)));
		if(d1 > 0.9995 && hapticOutcodes[rumblerID] != 1)
		{
			snap = 0.875f;
			hapticOutcodes[rumblerID] = 1;
		}
		else if(d1 < 0.0005 && hapticOutcodes[rumblerID] != -1)
		{
			snap = 1.0f;
			snoop = 0x7000;
			hapticOutcodes[rumblerID] = -1;
		} 
		if(d1 > 0.0015 && d1 < 0.9985 && hapticOutcodes[rumblerID] != 0)
		{
			if(hapticOutcodes[rumblerID] == 1)
				snap = 0.0075f;
			else
			{
				snap = 0.0075f;
				snoop = 0x0800;
			}
			hapticOutcodes[rumblerID] = 0;
		}

		double spd = Math.min(1.0, Math.sqrt(Math.pow(x1-x0, 2)+Math.pow(y1-y0, 2))*1E7/dt);

		int period = 0x1400 + snoop + (int) (0x4400*d1);
		int microseconds = (int) (50E3);
		hapticIntensity[rumblerID] = (int)(0xFFFF*(snap+(1-snap)*spd*(0.05+0.075*d1)));
		hapticPeriod[rumblerID] = period;
		hapticCount[rumblerID] = (microseconds/period);
	}

	protected void zeroHaptics(byte rumblerID) {
		hapticIntensity[rumblerID] = 0;
		hapticPeriod[rumblerID] = 0;
		hapticCount[rumblerID] = 0;
	}

	public void cleanup()
	{
		if(interfaceClaimed)
		{
			if(connected)
			{
				try {
					data.put( 0, SteamController.STEAM_CMD_DEFAULT_MAPPINGS);
					doControlTransfer(250L);
					data.put( 0, SteamController.STEAM_CMD_DEFAULT_MOUSE);
					doControlTransfer(250L);
				} catch(IOException err) {
					System.out.println("Info: Failed to reset steam controller mappings to default:");
					System.out.println(err.toString());
				}
				connected = false;
			}

			int result = LibUsb.releaseInterface(handle, device.interfaceNo);
			if (result != LibUsb.SUCCESS)
				System.out.println("Info: Unable to release interface: "+result+" (0x"+Integer.toHexString(result)+")");

			interfaceClaimed = false;
		}

		// Attach the kernel driver again if needed
		if (kernelDriver)
		{
			int result = LibUsb.attachKernelDriver(handle, device.interfaceNo);
			if (result != LibUsb.SUCCESS) 
				System.out.println("Info: Unable to re-attach kernel driver: "+result+" (0x"+Integer.toHexString(result)+")");
			kernelDriver = false;
		}

		if(handle != null)
		{
			LibUsb.close(handle);
			handle = null;
		}
		
		if(mouseUpdater != null)
			mouseUpdater.stop();
		
		System.out.println("Info: "+this+" cleaned up");
	}

	private void fetchData(byte[] dst)
	{
		data.get(dst);
		data.rewind();
	}

	private void addToClickQueue(SCButton b) {
		clickQueue[clickQueueHead] = b;
		clickQueueHead = (clickQueueHead+1)%clickQueue.length;
		if(clickQueueHead == clickQueueTail)
			clickQueueTail = (clickQueueTail+1)%clickQueue.length;//Discarded event
	}

	protected void doSetup()
	{
		if(!config.applyConfiguration)
			return;
		try {
			data.put( 0, SteamController.STEAM_CMD_CLEAR_MAPPINGS);
			doControlTransfer(250L);

			data.put( 0, SteamController.STEAM_CMD_WRITE_REGISTER);
			data.put( 1, (byte)12);//size (bytes)

			data.put( 2, SteamController.STEAM_REG_GYRO_MODE);
			data.put( 3, (byte)(config.gyroMode&0xFF));
			data.put( 4, (byte)(config.gyroMode>>>8));

			data.put( 5, SteamController.STEAM_REG_LSTICK_MODE);
			data.put( 6, (byte)(config.leftStickMode&0xFF));
			data.put( 7, (byte)(config.leftStickMode>>>8));

			data.put( 8, SteamController.STEAM_REG_RPAD_MODE);
			data.put( 9, (byte)(config.rightPadMode&0xFF));
			data.put(10, (byte)(config.rightPadMode>>>8));

			data.put(11, SteamController.STEAM_REG_TRACKBALL_OR_MARGIN);
			data.put(12, (byte)(config.trackballOrMargin&0xFF));
			data.put(13, (byte)(config.trackballOrMargin>>>8));

			doControlTransfer(250L);
		} catch (IOException err) {
			System.out.println("Info: Failed to apply custom steam controller configuration: ");
			System.out.println(err.getMessage());
		}
	}

	public void doForceFeedback(byte rumblerID, int intensity, int period, int count)
	{
		try {
			data.put(0, SteamController.STEAM_CMD_FORCEFEEDBAK);
			data.put(1, (byte)0x07);//Size in bytes
			data.put(2, (byte)rumblerID);
			data.put(3, (byte)(intensity&0xFF));
			data.put(4, (byte)(intensity>>>8));
			data.put(5, (byte)(period&0xFF));//(microseconds)
			data.put(6, (byte)(period>>>8));
			data.put(7, (byte)(count&0xFF));//(number of pulses)
			data.put(8, (byte)(count>>>8));
			doControlTransfer(1L);
		} catch (IOException err) {
			System.out.println("Info: Failed to send force feedback message: ");
			System.out.println(err.getMessage());
		}
	}

	protected boolean doInterruptTransfer(long timeout) throws IOException
	{
		int result = LibUsb.interruptTransfer(handle, device.endpoint, data, transferred, timeout);
		if(result == LibUsb.ERROR_TIMEOUT)
			return false;
		if(result != 0)
			throw new IOException("Interrupt transfer failed: "+result+" (0x"+Integer.toHexString(result)+")");
		return true;
	}

	public void doControlTransfer(long timeout) throws IOException
	{
		int result = LibUsb.controlTransfer(handle, (byte) (LibUsb.REQUEST_TYPE_CLASS|LibUsb.RECIPIENT_INTERFACE), 
				SteamController.HID_REQ_SET_REPORT, (short)0x0300, device.controlIndex, data, timeout);
		if(result != data.capacity())
		{
			throw new IOException("Control transfer failed: "+result+" (0x"+Integer.toHexString(result)+")");
		}
	}

	public void poll(SteamControllerData data) throws IOException {
		synchronized (lock) {
			while(clickQueueHead != clickQueueTail)
			{
				SCButton b = clickQueue[clickQueueTail];
				clickQueueTail = (clickQueueTail+1)%clickQueue.length;
				data.clickQueue.add(b);
			}
			System.arraycopy(lPadData, 0, data.lPadData, 0, 64);
			System.arraycopy(lStickData, 0, data.lStickData, 0, 64);
			data.latestData = lPadIsLatestData?data.lPadData:data.lStickData;
			data.lastUpdateTimeNanos = lastUpdateTimeNanos;
			if(fault != null)
				throw fault;
		}
	}

	public void rumble(int rumblerID, float intensity) {
		synchronized (lock) {
			vibration[rumblerID] = intensity;
			vibrationTimes[rumblerID] = System.nanoTime();
		}
	}

	public void setEventQueueSize(int size) {
		synchronized (lock) {
			clickQueue = new SCButton[size];
			clickQueueHead = 0;
			clickQueueTail = 0;
		}
	}
}
