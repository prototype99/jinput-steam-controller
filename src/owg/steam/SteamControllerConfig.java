package owg.steam;

import java.util.Properties;

import org.usb4java.Device;

import static owg.steam.SteamController.*;

public class SteamControllerConfig
{
	public final Device device;
	public final short pid;
	public final int portNo;

	public final byte endpoint;
	public final short controlIndex;
	public final int interfaceNo;
	
	public final boolean applyConfiguration;

	public final short leftStickMode;
	public final short rightPadMode;
	public final short trackballOrMargin;
	public final short gyroMode;

	public final boolean leftPadAutoHaptics;
	public final boolean rightPadAutoHaptics;

	public final float leftStickDeadZone;
	public final float leftPadDeadZone;
	public final float rightPadDeadZone;

	public final float leftStickEdgeZone;
	public final float leftPadEdgeZone;
	public final float rightPadEdgeZone;
	
	public final float gyroMouseX;
	public final float gyroMouseY;
	public final int gyroMouseEnableMask;
	public final int gyroMouseDisableMask;
	
	public final int buttonMask;
	public final boolean hideDisabledButtons;
	
	public SteamControllerConfig(Properties properties, Device device, short pid, int portNo, byte endpoint, short controlIndex, int interfaceNo)
	{
		this.device = device;
		this.pid = pid;
		this.portNo = portNo;
		
		this.endpoint = endpoint;
		this.controlIndex = controlIndex;
		this.interfaceNo = interfaceNo;
		
		
		this.applyConfiguration = SCUtil.getByte(properties, PROP_APPLY_CONFIGURATION, 0x01) != 0;

		this.leftStickMode = SCUtil.getShort(properties, PROP_LEFT_STICK_MODE, STEAM_INPUT_MODE_JOYSTICK);
		this.rightPadMode = SCUtil.getShort(properties, PROP_RIGHT_PAD_MODE, STEAM_INPUT_MODE_JOYSTICK);
		this.trackballOrMargin = SCUtil.getShort(properties, PROP_RIGHT_TRACKBALL_OR_MARGIN, rightPadMode==0?0x8000:0x0000);
		this.gyroMode = SCUtil.getShort(properties, PROP_GYRO_MODE, STEAM_GYRO_MODE_SEND_RAW_GYRO);

		this.leftPadAutoHaptics = SCUtil.getByte(properties, PROP_LEFT_PAD_AUTO_HAPTICS, BYTE_FALSE) != 0;
		this.rightPadAutoHaptics = SCUtil.getByte(properties, PROP_RIGHT_PAD_AUTO_HAPTICS, BYTE_TRUE) != 0 && rightPadMode != STEAM_INPUT_MODE_MOUSE;

		this.leftStickDeadZone = SCUtil.getFloat(properties, PROP_LEFT_STICK_DEAD_ZONE, 0.0f);
		this.leftPadDeadZone = SCUtil.getFloat(properties, PROP_LEFT_PAD_DEAD_ZONE, 0.2f);
		this.rightPadDeadZone = SCUtil.getFloat(properties, PROP_RIGHT_PAD_DEAD_ZONE, 0.2f);

		this.leftStickEdgeZone = SCUtil.getFloat(properties, PROP_LEFT_STICK_EDGE_ZONE, 0.0f);
		this.leftPadEdgeZone = SCUtil.getFloat(properties, PROP_LEFT_PAD_EDGE_ZONE, 0.3f);
		this.rightPadEdgeZone = SCUtil.getFloat(properties, PROP_RIGHT_PAD_EDGE_ZONE, 0.3f);
		
		this.gyroMouseX = SCUtil.getFloat(properties, PROP_GYRO_MOUSE_X, 0.0f);
		this.gyroMouseY = SCUtil.getFloat(properties, PROP_GYRO_MOUSE_Y, 0.0f);
		this.gyroMouseEnableMask = SCUtil.getInt(properties, PROP_GYRO_MOUSE_ENABLE_MASK, 0x0);
		this.gyroMouseDisableMask = SCUtil.getInt(properties, PROP_GYRO_MOUSE_DISABLE_MASK, 0x0);	
		
		this.buttonMask = SCUtil.getInt(properties, PROP_BUTTON_MASK, 0x7FFFFF);
		this.hideDisabledButtons = SCUtil.getByte(properties, PROP_HIDE_DISABLED_BUTTONS, BYTE_FALSE) != 0;
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
