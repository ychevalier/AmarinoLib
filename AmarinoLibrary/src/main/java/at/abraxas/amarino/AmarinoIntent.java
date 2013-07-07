/*
  Amarino - A prototyping software toolkit for Android and Arduino
  Copyright (c) 2010 Bonifaz Kaufmann.  All right reserved.

  This application and its library is free software; you can redistribute
  it and/or modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package at.abraxas.amarino;

public interface AmarinoIntent {

	// =============================
	// == From Helper to Service. ==
	// =============================

	/**
	 * Extras: EXTRA_DEVICE_ADDRESS
	 *
	 * Return: ACTION_CONNECTED, ACTION_DISCONNECTED, ACTION_CONNECTION_FAILED, ACTION_PAIRING_REQUESTED.
	 */
	public static final String ACTION_CONNECT = "amarino.intent.action.CONNECT";

	/**
	 * Extras: EXTRA_DEVICE_ADDRESS
	 *
	 * Return: ACTION_DISCONNECTED.
	 */
	public static final String ACTION_DISCONNECT = "amarino.intent.action.DISCONNECT";

	/**
	 * Extras: EXTRA_DEVICE_ADDRESS, EXTRA_DATA, EXTRA_DATA_TYPE, EXTRA_FLAG.
	 *
	 * Return: ACTION_RECEIVED.
	 */
	public static final String ACTION_SEND = "amarino.intent.action.SEND";

	/**
	 * Return: ACTION_NEARBY_DEVICES.
	 */
	public static final String ACTION_GET_NEARBY_DEVICES = "amarino.intent.action.GET_NEARBY_DEVICES";

	/**
	 * Return: ACTION_CONNECTED_DEVICES.
	 */
	public static final String ACTION_GET_CONNECTED_DEVICES = "amarino.intent.action.ACTION_GET_CONNECTED_DEVICES";

	// =============================
	// == From Service to Helper. ==
	// =============================

	/**
	 * Extras: EXTRA_DEVICE_ADDRESS, EXTRA_DATA, EXTRA_DATA_TYPE.
	 */
	public static final String ACTION_RECEIVED = "amarino.intent.action.RECEIVED";

	/**
	 * Extras: EXTRA_DEVICE_ADDRESS.
	 */
	public static final String ACTION_CONNECTED = "amarino.intent.action.CONNECTED";

	/**
	 * Extras: EXTRA_DEVICE_ADDRESS.
	 */
	public static final String ACTION_DISCONNECTED = "amarino.intent.action.DISCONNECTED";

	/**
	 * Extras: EXTRA_DEVICE_ADDRESS.
	 */
	public static final String ACTION_CONNECTION_FAILED = "amarino.intent.action.CONNECTION_FAILED";

	/**
	 * Extras: EXTRA_DEVICE_ADDRESS.
	 */
	public static final String ACTION_PAIRING_REQUESTED = "amarino.intent.action.PAIRING_REQUESTED";

	/**
	 * Extras: EXTRA_DEVICE_ADDRESSES.
	 */
	public static final String ACTION_CONNECTED_DEVICES = "amarino.intent.action.ACTION_CONNECTED_DEVICES";

	/**
	 * Extras: EXTRA_DEVICE_NAMES, EXTRA_DEVICE_ADDRESSES.
	 */
	public static final String ACTION_NEARBY_DEVICES = "amarino.intent.action.ACTION_NEARBY_DEVICES";

	// ====================
	// == Common Extras. ==
	// ====================

	/**
	 * Type: String array.
	 */
	public static final String EXTRA_DEVICE_NAMES = "amarino.intent.extra.DEVICE_NAMES";

	/**
	 * Type: String array.
	 */
	public static final String EXTRA_DEVICE_ADDRESSES = "amarino.intent.extra.DEVICE_ADDRESSES";

	/**
	 * Type: String.
	 */
	public static final String EXTRA_DEVICE_ADDRESS = "amarino.intent.extra.DEVICE_ADDRESS";

	/**
	 * Registered method to call on Arduino board.
	 *
	 * Type: char.
	 */
	public static final String EXTRA_FLAG = "amarino.intent.extra.FLAG";

	/**
	 * Type: String.
	 */
	public static final String EXTRA_DATA = "amarino.intent.extra.DATA";

	/**
	 * Type: Amarino Type (int).
	 */
	public static final String EXTRA_DATA_TYPE = "amarino.intent.extra.DATA_TYPE";

	// ==================
	// == Type Extras. ==
	// ==================

	/**
	 * boolean in Android is in Arduino 0=false, 1=true
	 */
	public static final int BOOLEAN_EXTRA = 1;
	public static final int BOOLEAN_ARRAY_EXTRA = 2;
	/**
	 * byte is byte. In Arduino a byte stores an 8-bit unsigned number, from 0
	 * to 255.
	 */
	public static final int BYTE_EXTRA = 3;
	public static final int BYTE_ARRAY_EXTRA = 4;
	/**
	 * char is char. In Arduino stored in 1 byte of memory
	 */
	public static final int CHAR_EXTRA = 5;
	public static final int CHAR_ARRAY_EXTRA = 6;
	/**
	 * double is too large for Arduinos, better not to use this datatype
	 */
	public static final int DOUBLE_EXTRA = 7;
	public static final int DOUBLE_ARRAY_EXTRA = 8;
	/**
	 * float in Android is float in Arduino (4 bytes)
	 */
	public static final int FLOAT_EXTRA = 9;
	public static final int FLOAT_ARRAY_EXTRA = 10;
	/**
	 * int in Android is long in Arduino (4 bytes)
	 */
	public static final int INT_EXTRA = 11;
	public static final int INT_ARRAY_EXTRA = 12;
	/**
	 * long in Android does not fit in Arduino data types, better not to use it
	 */
	public static final int LONG_EXTRA = 13;
	public static final int LONG_ARRAY_EXTRA = 14;
	/**
	 * short in Android is like int in Arduino (2 bytes) 2^15
	 */
	public static final int SHORT_EXTRA = 15;
	public static final int SHORT_ARRAY_EXTRA = 16;
	/**
	 * String in Android is char[] in Arduino
	 */
	public static final int STRING_EXTRA = 17;
	public static final int STRING_ARRAY_EXTRA = 18;
}
