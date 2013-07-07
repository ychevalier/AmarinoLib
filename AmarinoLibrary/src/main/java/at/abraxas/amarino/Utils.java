package at.abraxas.amarino;

import java.util.regex.Pattern;

public class Utils {

	/**
	 * Convenient method to check if a given Bluetooth address is in proper format.
	 *
	 * <p>A correct Bluetooth address has 17 charaters and the following format: xx:xx:xx:xx:xx:xx</p>
	 *
	 * @param address the address to prove
	 * @return true if the address is in proper format, otherwise false
	 */
	public static boolean isCorrectAddressFormat(String address){
		if (address == null || address.length() != 17) return false;
		// TODO use regular expression to check format needs more specific regex
		return Pattern.matches("[[A-F][0-9][:]]+", address.toUpperCase());
	}
}
