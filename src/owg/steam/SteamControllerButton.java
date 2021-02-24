package owg.steam;

import net.java.games.input.Component.Identifier.Button;

/**Describes the steam controller buttons and their layout in bitfields*/
public enum SteamControllerButton
{
	R2(0, Button._0, "Right trigger fully pressed"),
	L2(1, Button._1, "Left trigger fully pressed"),
	R1(2, Button._2, "Right shoulder button"),
	L1(3, Button._3, "Left shoulder button"),
	
	Y(4, Button.Y, "Face button Y"),
	B(5, Button.B, "Face button B"),
	X(6, Button.X, "Face button X"),
	A(7, Button.A, "Face button A"),
	
	LP_UP(8, Button.TOP, "LPad Up", "Top of left pad pressed down"),
	LP_RT(9, Button.RIGHT, "LPad Rt", "Right side of left pad pressed down"),
	LP_LT(10, Button.LEFT, "LPad Lt", "Left side of left pad pressed down"),
	LP_DN(11, Button.BASE, "LPad Dn", "Bottom of left pad pressed down"),
	
	BACK(12, Button.SELECT, "Back", "Left menu button"),
	STEAM(13, Button.MODE, "Steam", "Steam logo button"),
	START(14, Button.START, "Start", "Right menu button"),
	
	LG(15,Button._15, "Left grip button"),
	RG(16,Button._16, "Right grip button"),
	
	LP_PRESS(17,Button._17, "LPad Press", "Left pad pressed down"),
	RP_PRESS(18,Button._18, "RPad Press", "Right pad pressed down"),
	
	LP_TOUCH(19,Button._19, "LPad Touch", "Left pad touched"),
	RP_TOUCH(20,Button._20, "RPad Touch", "Right pad touched"),
	
	//Mystery unused bit here
	STICK_BTN(22,Button._22, "Stick Btn", "Joystick pressed down");
	
	public final String title, description;
	public final int bitIndex;
	public final Button jinputButton;
	
	SteamControllerButton(int bit, Button btn, String description)
	{
		this(bit, btn, null, description);
	}
	
	SteamControllerButton(int bit, Button btn,  String title, String description)
	{
		this.bitIndex = bit;
		this.jinputButton = btn;
		this.title = title==null ? name() : title;
		this.description = description;
	}
}
