package owg.steam;

import java.util.LinkedList;
import java.util.Queue;

public class SteamControllerData
{
	/**Queue of unprocessed button presses/releases*/
	protected Queue<SCButton> clickQueue = new LinkedList<SCButton>();

	/**Data specific to left pad*/
	protected final byte[] lPadData = new byte[64];
	/**Data specific to left stick*/
	protected final byte[] lStickData = new byte[64];
	/**Latest data received from USB. Points to either lPadDataClient or lStickDataClient.*/
	protected byte[] latestData = lPadData;
	/**Timestamp for latest data*/
	protected long lastUpdateTimeNanos = Long.MIN_VALUE;
}
