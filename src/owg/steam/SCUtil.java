package owg.steam;

import java.util.Properties;

/**Uninteresting implementation details*/
public class SCUtil 
{
	/**Gets a 16 bit signed little endian value from given array starting at the given offset, and converts it to a float.*/
	protected static float val16(byte[] data, int i)
	{
		return ((short)(data[i  ]&0xFF | (data[i+1]&0xFF)<<8))/32768f;
	}
	/**Gets a byte value from the properties, or a default value if none or invalid.*/
	protected static byte getByte(Properties properties, String propName, int defaultVal)
	{
		String val = properties==null?null:properties.getProperty(propName);
		if(val == null)
			return (byte)defaultVal;
		try {
			int[] radix = new int[1];
			val = getRadix(val, radix);
			return (byte) Short.parseShort(val, radix[0]);
		} catch(NumberFormatException err) {
			System.out.println("Info: Property \""+propName+"\" should be a byte, got: \""+val+"\"");
			return (byte)defaultVal;
		}
	}

	/**Gets a short value from the properties, or a default value if none or invalid.*/
	protected static short getShort(Properties properties, String propName, int defaultVal)
	{
		String val = properties==null?null:properties.getProperty(propName);
		if(val == null)
			return (short)defaultVal;
		try {
			int[] radix = new int[1];
			val = getRadix(val, radix);
			return (short) Integer.parseInt(val, radix[0]);
		} catch(NumberFormatException err) {
			System.out.println("Info: Property \""+propName+"\" should be a short, got: \""+val+"\"");
			return (short)defaultVal;
		}
	}

	/**Gets an int value from the properties, or a default value if none or invalid.*/
	protected static int getInt(Properties properties, String propName, int defaultVal)
	{
		String val = properties==null?null:properties.getProperty(propName);
		if(val == null)
			return defaultVal;
		try {
			int[] radix = new int[1];
			val = getRadix(val, radix);
			return (int) Long.parseLong(val, radix[0]);
		} catch(NumberFormatException err) {
			System.out.println("Info: Property \""+propName+"\" should be an integer, got: \""+val+"\"");
			return defaultVal;
		}
	}

	/**Gets a float value from the properties, or a default value if none or invalid.*/
	protected static float getFloat(Properties properties, String propName, double defaultVal)
	{
		String val = properties==null?null:properties.getProperty(propName);
		if(val == null)
			return (float)defaultVal;
		try {
			return Float.valueOf(val);
		} catch(NumberFormatException err) {
			System.out.println("Info: Property \""+propName+"\" should be a float, got: \""+val+"\"");
			return (float)defaultVal;
		}
	}

	/**Trims any radix prefix from the integer string value and puts the radix number at the first index in the given array.*/
	protected static String getRadix(String val, int[] radix)
	{
		if(val.startsWith("0b"))
		{
			radix[0] = 2;
			return val.substring(2);
		}
		if(val.startsWith("0o"))
		{
			radix[0] = 8;
			return val.substring(2);
		}
		if(val.startsWith("0x"))
		{
			radix[0] = 16;
			return val.substring(2);
		}
		radix[0] = 10;
		return val;
	}
}
