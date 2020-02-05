package owg.steam;

/**Class representing a Steam Controller button that has been disabled, but not hidden by the application configuration.
 * @see SteamController#PROP_BUTTON_MASK
 * @see SteamController#PROP_HIDE_DISABLED_BUTTONS*/
public class DisabledSCButton extends SCButton
{
	protected DisabledSCButton(String name, Identifier id, int bit)
	{
		super(name, id, bit);
	}
	@Override
	public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData) {
		return 0.0f;
	}
}