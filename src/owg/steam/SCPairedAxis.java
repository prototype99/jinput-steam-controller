package owg.steam;

/**Class representing a Steam Controller axis that is paired with a different axis to form a 2-dimensional input 
 * (left stick, left touch pad and right touch pad).*/
public abstract class SCPairedAxis extends SCComponent
{
	protected SCPairedAxis other;

	protected SCPairedAxis(String name, Identifier id)
	{
		super(name, id, false);
	}
	/**Gets the axis that this axis is paired with.*/
	public SCPairedAxis getOther()
	{
		return other;
	}

	protected void pair(SCPairedAxis other)
	{
		this.other = other;
		other.other = this;
	}
	protected abstract float deadZone();
	protected abstract float edgeZone();
	protected abstract float pollRaw(byte[] lPadData, byte[] lStickData, byte[] latestData);
	
	@Override
	public float pollFrom(byte[] lPadData, byte[] lStickData, byte[] latestData)
	{
		float primary = pollRaw(lPadData, lStickData, latestData);
		float secondary = other.pollRaw(lPadData, lStickData, latestData);
		double src = Math.sqrt(primary*primary+secondary*secondary);
		if(src <= deadZone())
			return 0.0f;

		double div = 1.0-deadZone()-edgeZone();
		if(div <= 0)
		{
			//Normalize
			return (float) (primary * 1.0/src); 
		}
		else
		{
			double dst = Math.min(1.0, (src-deadZone())/div);
			return (float) (primary * dst/src);
		}

	}
}