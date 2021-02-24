package owg.steam;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Properties;

import static owg.steam.SteamController.*;

public class SteamControllerConfig
{
	public static DecimalFormat floatFormatter = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	// Notice: This class avoids any references to SteamController or SteamControllerPlugin,
	// so that the controller thread will not keep the plugin alive if the application stops using it.
	
	public boolean applyConfiguration;

	public short leftStickMode;
	public short rightPadMode;
	public short trackballOrMargin;
	public short gyroMode;

	public boolean leftPadAutoHaptics;
	public boolean rightPadAutoHaptics;

	public float leftStickDeadZone;
	public float leftPadDeadZone;
	public float rightPadDeadZone;

	public float leftStickEdgeZone;
	public float leftPadEdgeZone;
	public float rightPadEdgeZone;
	
	public float gyroMouseX;
	public float gyroMouseY;
	public int gyroMouseEnableMask;
	public int gyroMouseDisableMask;
	
	public int buttonMask;
	public boolean hideDisabledButtons;
	
	public SteamControllerConfig(Properties properties)
	{
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
	
	public void writeTo(Properties properties)
	{
		properties.setProperty(PROP_APPLY_CONFIGURATION, SCUtil.toHexString(applyConfiguration));
		
		properties.setProperty(PROP_LEFT_STICK_MODE, SCUtil.toHexString(leftStickMode));
		properties.setProperty(PROP_RIGHT_PAD_MODE, SCUtil.toHexString(rightPadMode));
		properties.setProperty(PROP_RIGHT_TRACKBALL_OR_MARGIN, SCUtil.toHexString(trackballOrMargin));
		properties.setProperty(PROP_GYRO_MODE, SCUtil.toHexString(gyroMode));

		properties.setProperty(PROP_LEFT_PAD_AUTO_HAPTICS, SCUtil.toHexString(leftPadAutoHaptics));
		properties.setProperty(PROP_RIGHT_PAD_AUTO_HAPTICS, SCUtil.toHexString(rightPadAutoHaptics));

		properties.setProperty(PROP_LEFT_STICK_DEAD_ZONE, floatFormatter.format(leftStickDeadZone));
		properties.setProperty(PROP_LEFT_PAD_DEAD_ZONE, floatFormatter.format(leftPadDeadZone));
		properties.setProperty(PROP_RIGHT_PAD_DEAD_ZONE, floatFormatter.format(rightPadDeadZone));
		
		properties.setProperty(PROP_LEFT_STICK_EDGE_ZONE, floatFormatter.format(leftStickEdgeZone));
		properties.setProperty(PROP_LEFT_PAD_EDGE_ZONE, floatFormatter.format(leftPadEdgeZone));
		properties.setProperty(PROP_RIGHT_PAD_EDGE_ZONE, floatFormatter.format(rightPadEdgeZone));

		properties.setProperty(PROP_GYRO_MOUSE_X, floatFormatter.format(gyroMouseX));
		properties.setProperty(PROP_GYRO_MOUSE_Y, floatFormatter.format(gyroMouseY));

		properties.setProperty(PROP_GYRO_MOUSE_ENABLE_MASK, SCUtil.toBinaryString(gyroMouseEnableMask, 23));
		properties.setProperty(PROP_GYRO_MOUSE_DISABLE_MASK, SCUtil.toBinaryString(gyroMouseDisableMask, 23));

		properties.setProperty(PROP_BUTTON_MASK, SCUtil.toBinaryString(buttonMask, 23));
		properties.setProperty(PROP_HIDE_DISABLED_BUTTONS, SCUtil.toHexString(hideDisabledButtons));
	}
}
