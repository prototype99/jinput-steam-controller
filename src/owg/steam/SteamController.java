package owg.steam;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import org.usb4java.Device;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import net.java.games.input.AbstractController;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.Event;

import static owg.steam.SteamControllerButton.*;
/**{@link Controller} implementation representing a Steam Controller.
 * <br><br>
 * Special thanks to the following people for publishing their work on reverse engineering the Steam Controller USB HID Protocol<ul>
 * <li>Rodrigo Rivas Costa</li>
 * <li>Stany MARCEL</li>
 * <li>Fighter19</li>
 * <li>XanClic</li>
 * </ul>*/
public class SteamController extends AbstractController
{
	public static final short PID_WIRELESS = 0x1142;
	public static final short PID_WIRED = 0x1102;

	public static final byte EV_INPUT_DATA = 0x01;
	public static final byte EV_WIRELESS_CONNECT = 0x03;
	public static final byte EV_BATTERY_STATUS = 0x04;

	public static final byte STEAM_WIRELESS_DISCONNECT = (byte)0x01;
	public static final byte STEAM_WIRELESS_CONNECT = (byte)0x02;

	public static final byte HID_REQ_SET_REPORT = (byte)0x09;

	public static final byte STEAM_CMD_CLEAR_MAPPINGS = (byte)0x81;	
	public static final byte STEAM_CMD_DEFAULT_MAPPINGS = (byte)0x85;	
	public static final byte STEAM_CMD_WRITE_REGISTER = (byte)0x87;
	public static final byte STEAM_CMD_DEFAULT_MOUSE = (byte)0x8e;
	public static final byte STEAM_CMD_FORCEFEEDBAK = (byte)0x8f;
	public static final byte STEAM_CMD_REQUEST_CONNECTION_STATUS = (byte)0xb4;

	public static final byte STEAM_REG_LSTICK_MODE = (byte)0x07;
	public static final byte STEAM_REG_RPAD_MODE = (byte)0x08;
	public static final byte STEAM_REG_TRACKBALL_OR_MARGIN = (byte)0x18;
	public static final byte STEAM_REG_GYRO_MODE = (byte)0x30;

	public static final short STEAM_GYRO_MODE_OFF = (short)0x0000;
	public static final short STEAM_GYRO_MODE_STEERING = (short)0x0001;
	public static final short STEAM_GYRO_MODE_TILT = (short)0x0002;
	public static final short STEAM_GYRO_MODE_SEND_ORIENTATION = (short)0x0004;
	public static final short STEAM_GYRO_MODE_SEND_RAW_ACCEL = (short)0x0008;
	public static final short STEAM_GYRO_MODE_SEND_RAW_GYRO = (short)0x0010;

	public static final short STEAM_INPUT_MODE_MOUSE = (short)0x0000;
	public static final short STEAM_INPUT_MODE_JOYSTICK = (short)0x0001;

	public static final byte STEAM_RUMBLER_LEFT = (byte)0x01;
	public static final byte STEAM_RUMBLER_RIGHT = (byte)0x00;

	public static final byte BYTE_TRUE = (byte)0x01;
	public static final byte BYTE_FALSE = (byte)0x00;

	protected static final Controller[] NO_CHILDREN = new Controller[0];
	protected static final SCRumbler[] NO_RUMBLERS = new SCRumbler[0];

	/**buttonMask is a bitfield with 23 bits, which can be used to enable (1) or disable (0) buttons.<br>
	 * If hideDisabledButtons is true, then disabled buttons will not be visible to the application.<br>
	 * Otherwise, they will be visible but never appear to be pressed.<br>
	 * <br>
	 * The default value is 0b11111111111111111111111, all buttons enabled.<br>
	 * <br>
	 * Bits are from least to most significant ("little endian", read: right to left), 
	 * in the native order of the device:<pre>
	 * Bit index  Button name
	 * 0          right trigger fully pressed
	 * 1          left trigger fully pressed
	 * 2          right shoulder button
	 * 3          left shoulder button
	 * 4          Y
	 * 5          B
	 * 6          X
	 * 7          A
	 * 8          left pad up
	 * 9          left pad right
	 * 10         left pad left
	 * 11         left pad down
	 * 12         menu left
	 * 13         steam logo
	 * 14         menu right
	 * 15         left grip button
	 * 16         right grip button
	 * 17         left pad clicked
	 * 18         right pad clicked
	 * 19         left pad touched
	 * 20         right pad touched
	 * 21         unused
	 * 22         joystick clicked
	 * </pre>
	 * For example, the value 0b11001111111111111111111 can be used to disable the "pad touched" buttons. 
	 * */	
	public static final String PROP_BUTTON_MASK = SteamController.class.getName()+".buttonMask";
	/**hideDisabledButtons can be set to 0 or 1 (default 0).<br>
	 * {@link #BYTE_FALSE}: Disabled buttons will be visible to the application, but never be pressed.<br>
	 * {@link #BYTE_TRUE}: Disabled buttons will not be visible to the application.*/
	public static final String PROP_HIDE_DISABLED_BUTTONS = SteamController.class.getName()+".hideDisabledButtons";
	
	/**applyConfiguration can be set to 0 or 1 (default 1):<br>
	 * {@link #BYTE_FALSE}: The leftStickMode, rightPadMode, rightPadTrackball and gyroMode properties are not applied.<br>
	 * {@link #BYTE_TRUE}: The Steam Controller's configuration will be changed by the software.*/
	public static final String PROP_APPLY_CONFIGURATION = SteamController.class.getName()+".applyConfiguration";
	/**leftStickMode can be set to 0 or 1 (default 1):<br>
	 * {@link #STEAM_INPUT_MODE_MOUSE}: The left stick will move the mouse pointer relative to the center of the screen.<br>
	 * {@link #STEAM_INPUT_MODE_JOYSTICK}: The left stick will not move the mouse pointer.*/
	public static final String PROP_LEFT_STICK_MODE = SteamController.class.getName()+".leftStickMode";
	/**rightPadMode can be set to 0 or 1 (default 1):<br>
	 * {@link #STEAM_INPUT_MODE_MOUSE}: The right pad will behave as a touchpad, moving the mouse pointer.<br>
	 * {@link #STEAM_INPUT_MODE_JOYSTICK}: The right pad will not move the mouse pointer.*/
	public static final String PROP_RIGHT_PAD_MODE = SteamController.class.getName()+".rightPadMode";
	/**trackballOrMargin can be set to 0 or 1 (default 1, no effect if rightPadMode is 1):<br>
	 * {@link #BYTE_FALSE}: The mouse pointer will not continue to move after flicking the right pad.<br>
	 * {@link #BYTE_TRUE}: The mouse pointer will have momentum and keep moving after flicking the right pad.*/
	public static final String PROP_RIGHT_TRACKBALL_OR_MARGIN = SteamController.class.getName()+".trackballOrMargin";
	/**gyroMode enables or disables the accelerometer and gyro (default 0x0010):<br>
	 * {@link #STEAM_GYRO_MODE_OFF}: The gyro and accelerometer are disabled.<br>
	 * {@link #STEAM_GYRO_MODE_SEND_RAW_GYRO}: The gyro is enabled.<br>
	 * {@link #STEAM_GYRO_MODE_SEND_RAW_ACCEL}: The accelerometer is enabled.<br>
	 * {@link #STEAM_GYRO_MODE_SEND_RAW_ACCEL}|{@link #STEAM_GYRO_MODE_SEND_RAW_GYRO}: The gyro and accelerometer are enabled.<br>
	 * Other values are possible, but not particularly useful to this plugin.*/	
	public static final String PROP_GYRO_MODE = SteamController.class.getName()+".gyroMode";
	/**rumble can be set to 0 or 1 (default 1):<br>
	 * {@link #BYTE_FALSE}: Rumblers are not made available to the application.<br>
	 * {@link #BYTE_TRUE}: The application can control the left and right haptics as if they were vibration motors.*/
	public static final String PROP_RUMBLERS = SteamController.class.getName()+".rumblers";
	/**Auto haptics can be set to 0 or 1 (default 0 for left, 1 for right):<br>
	 * {@link #BYTE_FALSE}: Automatic haptic feedback will not be generated when using the touch pad.<br>
	 * {@link #BYTE_TRUE}: Automatic haptic feedback will be generated when using the touch pad.<br>
	 * This has no effect for the right pad if rightPadMode is {@link #STEAM_INPUT_MODE_MOUSE}.*/
	public static final String PROP_LEFT_PAD_AUTO_HAPTICS = SteamController.class.getName()+".leftPadAutoHaptics",
			PROP_RIGHT_PAD_AUTO_HAPTICS = SteamController.class.getName()+".rightPadAutoHaptics";
	/**The dead zone indicates the radius of a circle at the center of the stick or pad,<br>
	 * where the input will be treated as zero (and no action will happen in the game).<br>
	 * The radius is given as a number between 0 (no dead zone) and 1 (dead zone covers the entire device).*/
	public static final String PROP_LEFT_STICK_DEAD_ZONE = SteamController.class.getName()+".leftStickDeadZone",
			PROP_LEFT_PAD_DEAD_ZONE = SteamController.class.getName()+".leftPadDeadZone",
			PROP_RIGHT_PAD_DEAD_ZONE = SteamController.class.getName()+".rightPadDeadZone";
	/**The edge zone indicates the distance between the edge of the stick or pad to an imaginary circle,<br>
	 * where any input outside of this circle will be treated as the maximum amplitude.<br>
	 * The radius is given as a number between 0 (no edge zone) and 1 (edge zone covers the entire device).*/	
	public static final String PROP_LEFT_STICK_EDGE_ZONE = SteamController.class.getName()+".leftStickEdgeZone",
			PROP_LEFT_PAD_EDGE_ZONE = SteamController.class.getName()+".leftPadEdgeZone",
			PROP_RIGHT_PAD_EDGE_ZONE = SteamController.class.getName()+".rightPadEdgeZone";
	
	/**Controls how much the gyro influences the mouse. Set to zero to disable.*/
	public static final String PROP_GYRO_MOUSE_X = SteamController.class.getName()+".gyroMouseX",
			PROP_GYRO_MOUSE_Y = SteamController.class.getName()+".gyroMouseY";
	
	/**Bits for buttons that can be pressed to enable gyro mouse control. 
	 * Set to zero to not require any buttons. The bitmask layout is the same as for buttonMask.*/
	public static final String PROP_GYRO_MOUSE_ENABLE_MASK = SteamController.class.getName()+".gyroMouseEnableMask";
	/**Bits for buttons that can be pressed to disable gyro mouse control. The bitmask layout is the same as for buttonMask.*/
	public static final String PROP_GYRO_MOUSE_DISABLE_MASK = SteamController.class.getName()+".gyroMouseDisableMask";

	/**Properties object for configuring SteamController instances.<br>
	 * <br>
	 * The default value of this field is {@link System#getProperties()}, but it may be changed. It is safe to set this to <code>null</code> to use default values only, 
	 * but it is recommended that the application allows the user to change this object via some mechanism 
	 * (by default, properties can be injected into the system properties with JVM arguments, e.g. <code>-Dowg.steam.SteamController.gyroMode=0x00</code>).<br>
	 * <br>
	 * Values are read from this object only on controller creation, that is when the {@link SteamControllerPlugin} constructor is called.<br>
	 * <br>
	 * See <code>PROP</code> constants for valid keys.*/
	public static Properties properties = System.getProperties();

	/**GC prevention: The plugin should kept alive as long as there is a strong reference to {@link SteamController} or {@link SteamControllerPlugin}.*/
	public final SteamControllerPlugin env;

	protected SteamControllerData data;
	protected SteamControllerConfig config;
	protected SteamControllerDevice device;
	public final SteamControllerThreadTask threadTask;

	public SteamController(SteamControllerPlugin env, Device device, short pid, int interfaceNo, int endpointIndex) throws LibUsbException
	{
		super("Steam Controller"+(pid == PID_WIRELESS?" "+interfaceNo+" (wireless)":""), componentArray(), NO_CHILDREN, 
				SCUtil.getByte(properties, PROP_RUMBLERS, 0x01) == 0 ? NO_RUMBLERS : rumblerArray());
		this.data = new SteamControllerData();
		this.config = new SteamControllerConfig(properties);
		this.device= new SteamControllerDevice(device, pid, LibUsb.getPortNumber(device), (byte)(LibUsb.ENDPOINT_IN|endpointIndex), (short)interfaceNo, interfaceNo);
		for(SCComponent c : (SCComponent[])getComponents())
		{
			c.data = data;
			c.config = config;
		}
		for(SCRumbler c : (SCRumbler[])getRumblers())
			c.host = this;
		this.env = env;

		threadTask = new SteamControllerThreadTask(this);
	}

	protected static SCRumbler[] rumblerArray()
	{
		SCRumbler[] r = new SCRumbler[2];
		r[0] = new SCRumbler("Left Motor", Identifier.Axis.X_FORCE, STEAM_RUMBLER_LEFT);
		r[1] = new SCRumbler("Right Motor", Identifier.Axis.RX, STEAM_RUMBLER_RIGHT);
		return r;
	}

	protected static SCComponent[] componentArray()
	{
		int bm = SCUtil.getInt(properties, PROP_BUTTON_MASK, 0x7FFFFF);
		boolean hd = SCUtil.getByte(properties, PROP_HIDE_DISABLED_BUTTONS, BYTE_FALSE) != 0;
		
		short gyroMode = SCUtil.getShort(properties, PROP_GYRO_MODE, STEAM_GYRO_MODE_SEND_RAW_GYRO);
		boolean accel = (gyroMode&STEAM_GYRO_MODE_SEND_RAW_ACCEL) != 0;
		boolean gyro = (gyroMode&STEAM_GYRO_MODE_SEND_RAW_GYRO) != 0;
		SCComponent[] r = new SCComponent[36];
		int i = 0;
		i += newButton(r, i, bm, hd, R2);
		i += newButton(r, i, bm, hd, L2);
		i += newButton(r, i, bm, hd, R1);
		i += newButton(r, i, bm, hd, L1);

		i += newButton(r, i, bm, hd, A);
		i += newButton(r, i, bm, hd, B);
		i += newButton(r, i, bm, hd, X);
		i += newButton(r, i, bm, hd, Y);

		i += newButton(r, i, bm, hd, LP_UP);
		i += newButton(r, i, bm, hd, LP_RT);
		i += newButton(r, i, bm, hd, LP_LT);
		i += newButton(r, i, bm, hd, LP_DN);

		i += newButton(r, i, bm, hd, BACK);
		i += newButton(r, i, bm, hd, STEAM);
		i += newButton(r, i, bm, hd, START);
		
		i += newButton(r, i, bm, hd, LG);
		i += newButton(r, i, bm, hd, RG);
		
		i += newLPButton(r, i, bm, hd, LP_PRESS);
		i += newButton(r, i, bm, hd, RP_PRESS);
		
		i += newButton(r, i, bm, hd, LP_TOUCH);
		i += newButton(r, i, bm, hd, RP_TOUCH);
		
		//Mystery unused bit at 21
		i += newButton(r, i, bm, hd, STICK_BTN);
		//LPad/Stick conjunction bit at 23

		r[i  ] = new SCPairedAxis("X Axis", Identifier.Axis.X)
		{
			@Override
			public float pollRaw(byte[] lPadData, byte[] lStickData, byte[] latestData)
			{
				return SCUtil.val16(lStickData, 16);
			}
			@Override
			protected float deadZone()
			{
				return config.leftStickDeadZone;
			}
			@Override
			protected float edgeZone()
			{
				return config.leftStickEdgeZone;
			}
		};
		r[i+1] = new SCPairedAxis("Y Axis", Identifier.Axis.Y)
		{
			@Override
			public float pollRaw(byte[] lPadData, byte[] lStickData, byte[] latestData)
			{
				return -SCUtil.val16(lStickData, 18);
			}
			@Override
			protected float deadZone()
			{
				return config.leftStickDeadZone;
			}
			@Override
			protected float edgeZone()
			{
				return config.leftStickEdgeZone;
			}
		};
		((SCPairedAxis)r[i  ]).pair((SCPairedAxis)r[i+1]);
		i+=2;

		r[i  ] = new SCPairedAxis("LPad X", Identifier.Axis.X_FORCE)
		{
			@Override
			public float pollRaw(byte[] lPadData, byte[] lStickData, byte[] latestData)
			{
				return SCUtil.val16(lPadData, 16);
			}
			@Override
			protected float deadZone()
			{
				return config.leftPadDeadZone;
			}
			@Override
			protected float edgeZone()
			{
				return config.leftPadEdgeZone;
			}
		};
		r[i+1] = new SCPairedAxis("LPad Y", Identifier.Axis.Y_FORCE)
		{
			@Override
			public float pollRaw(byte[] lPadData, byte[] lStickData, byte[] latestData)
			{
				return -SCUtil.val16(lPadData, 18);
			}
			@Override
			protected float deadZone()
			{
				return config.leftPadDeadZone;
			}
			@Override
			protected float edgeZone()
			{
				return config.leftPadEdgeZone;
			}
		};
		((SCPairedAxis)r[i  ]).pair((SCPairedAxis)r[i+1]);
		i+=2;

		r[i  ] = new SCPairedAxis("RPad X", Identifier.Axis.RX_FORCE)
		{
			@Override
			public float pollRaw(byte[] lPadData, byte[] lStickData, byte[] latestData)
			{
				return SCUtil.val16(latestData, 20);
			}
			@Override
			protected float deadZone()
			{
				return config.rightPadDeadZone;
			}
			@Override
			protected float edgeZone()
			{
				return config.rightPadEdgeZone;
			}
		};
		r[i+1] = new SCPairedAxis("RPad Y", Identifier.Axis.RY_FORCE)
		{
			@Override
			public float pollRaw(byte[] lPadData, byte[] lStickData, byte[] latestData)
			{
				return -SCUtil.val16(latestData, 22);
			}
			@Override
			protected float deadZone()
			{
				return config.rightPadDeadZone;
			}
			@Override
			protected float edgeZone()
			{
				return config.rightPadEdgeZone;
			}
		};
		((SCPairedAxis)r[i  ]).pair((SCPairedAxis)r[i+1]);
		i+=2;

		r[i] = new SCComponent("LT", Identifier.Axis.Z_FORCE, false)
		{			
			@Override
			public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
			{
				return (latestData[11]&0xFF)/255.0f;
			}
		};
		i++;
		r[i] = new SCComponent("RT", Identifier.Axis.RZ_FORCE, false)
		{			
			@Override
			public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
			{
				return (latestData[12]&0xFF)/255.0f;
			}
		};
		i++;

		if(gyro)
		{
			r[i] = new SCComponent("Gyro X", Identifier.Axis.RX, false)
			{			
				@Override
				public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
				{
					return SCUtil.val16(latestData, 34);
				}
			};
			i++;
			r[i] = new SCComponent("Gyro Y", Identifier.Axis.RY, false)
			{			
				@Override
				public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
				{
					return SCUtil.val16(latestData, 36);
				}
			};
			i++;
			r[i] = new SCComponent("Gyro Z", Identifier.Axis.RZ, false)
			{			
				@Override
				public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
				{
					return -SCUtil.val16(latestData, 38);
				}
			};
			i++;
		}
		if(accel)
		{
			r[i] = new SCComponent("Accel X", Identifier.Axis.X_ACCELERATION, false)
			{			
				@Override
				public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
				{
					return SCUtil.val16(latestData, 28);
				}
			};
			i++;
			r[i] = new SCComponent("Accel Y", Identifier.Axis.Y_ACCELERATION, false)
			{			
				@Override
				public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
				{
					return SCUtil.val16(latestData, 30);
				}
			};
			i++;
			r[i] = new SCComponent("Accel Z", Identifier.Axis.Z_ACCELERATION, false)
			{			
				@Override
				public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
				{
					return SCUtil.val16(latestData, 32);
				}
			};
			i++;
		}
		if(i<r.length)
			return Arrays.copyOf(r, i);
		else
			return r;
	}

	private static int newButton(SCComponent[] r, int i, int buttonMask, boolean hideDisabled, SteamControllerButton button) 
	{
		if((buttonMask&(1<<button.bitIndex)) != 0)
		{
			r[i] = new SCButton(button.title, button.jinputButton, button.bitIndex);
			return 1;
		}
		else if(hideDisabled)
			return 0;
		else
		{
			r[i] = new DisabledSCButton(button.title, button.jinputButton, button.bitIndex);
			return 1;
		}
	}
	private static int newLPButton(SCComponent[] r, int i, int buttonMask, boolean hideDisabled, SteamControllerButton button) 
	{
		if((buttonMask&(1<<button.bitIndex)) != 0)
		{
			r[i] = new SCButton(button.title, button.jinputButton, button.bitIndex)
			{
				@Override
				public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData) {
					//It is located at bit 17, but when lpad is not touched, 
					//it duplicates the analog stick click for some reason!!
					//Need to get value from lPadData
					return ((lPadData[8+byteOffset]&bitMask) == bitMask) ? 1.0f : 0.0f;
				}
			};
			return 1;
		}
		else if(hideDisabled)
			return 0;
		else
		{
			r[i] = new DisabledSCButton(button.title, button.jinputButton, button.bitIndex);
			return 1;
		}
	}

	@Override
	public String toString()
	{
		return getName()+" "+device.portNo+"-"+device.interfaceNo;
	}

	@Override
	protected void pollDevice() throws IOException
	{
		threadTask.poll(data);
	}

	@Override
	protected boolean getNextDeviceEvent(Event event) throws IOException
	{
		SCButton b;
		while((b = data.clickQueue.poll()) != null)
		{
			b.cachedValue = 1.0f-b.cachedValue;
			event.set(b, b.cachedValue, data.lastUpdateTimeNanos);
			return true;
		}

		for(SCComponent c : (SCComponent[])getComponents())
		{
			if(!(c instanceof SCButton))
			{
				float pv = c.getPollData();
				if(pv != c.cachedValue)
				{
					c.cachedValue = pv;
					event.set(c, pv, data.lastUpdateTimeNanos);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Type getType()
	{
		return Type.GAMEPAD;
	}

	@Override
	public PortType getPortType()
	{
		return PortType.USB;
	}

	@Override
	public int getPortNumber()
	{
		return device.portNo;
	}

	@Override
	protected void setDeviceEventQueueSize(int size) throws IOException
	{
		threadTask.setEventQueueSize(size);
	}
}
