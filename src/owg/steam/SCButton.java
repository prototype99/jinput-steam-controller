package owg.steam;

/**Base class for Steam Controller button implementations.*/
public class SCButton extends SCComponent
{
	public final int byteOffset;
	public final int bitMask;
	/**The last processed poll value, owned by the controller thread!*/
	protected float latestValue;

	protected SCButton(String name, Identifier id, int bit)
	{
		super(name, id, false);
		this.byteOffset = bit/8;
		this.bitMask = 1<<(bit%8);
	}
	@Override
	public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
	{
		return ((latestData[8+byteOffset]&bitMask) == bitMask) ? 1.0f : 0.0f;
	}
}