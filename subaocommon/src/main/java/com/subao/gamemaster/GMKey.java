package com.subao.gamemaster;

@Deprecated
public class GMKey {

    private GMKey() {

    }

	/**
	 * Get the network delay from gamemaster engine, long value will be returned
	 * whose unit is millisecond.
	 * 
	 * @see {@link GameMaster#getLong(int)}
	 * */
    @Deprecated
    public static final int SGM_GET_NETDELAY = 100;

	/**
	 * Get the server node from gamemaster engine, string value will be returned
	 * 
	 * @see {@link GameMaster#getString(int)}
	 * */
	@Deprecated
	public static final int SGM_GET_SERVER_NODE = 101;

	/**
	 * Get accelerator status, if switch on, 1 will be returned, and if switch
	 * off, 0 will be returned
	 * 
	 * @see {@link GameMaster#getLong(int)}
	 * */
    @Deprecated
	public static final int SGM_GET_ACCEL_STAT = 102;

	/**
	 * Get the data flow of the game used this month, be metered by MB
	 * 
	 * @see {@link GameMaster#getLong(int)}
	 * */
	@Deprecated
	public static final int SGM_GET_GAME_FLOW = 103;

	/**
	 * Get autostart status, 1 represent autostart is on, 0 on the other hand
	 * 
	 * @see {@link GameMaster#getLong(int)}
	 * @see {@link #SGM_SET_AUTO_START}
	 * */
	@Deprecated
	public static final int SGM_GET_AUTO_START = 104;

	/**
	 * Get main window status, 1 represent main window has shown, 0 represent
	 * not shown
	 * 
	 * @see {@link GameMaster#getLong(int)}
	 * */
	@Deprecated
	public static final int SGM_GET_BIG_SHOWN = 105;

	/**
	 * Get connection repair success count
	 * 
	 * @see {@link GameMaster#getLong(int)}
	 * */
	@Deprecated
	public static final int SGM_GET_REPAIR_SUCCESS_COUNT = 106;

	/**
	 * Get current accelerate effect status, a string like "75%" will be
	 * returned, if no accelerate effect, "---" will be returned
	 * 
	 * @see {@link GameMaster#getString(int)}
	 * */
	@Deprecated
	public static final int SGM_GET_ACCEL_EFFECT = 107;

	/**
	 * Get current SDK version code, long value will be returned, and this value
	 * always be positive integer, and always ascending
	 * 
	 * @see {@link GameMaster#getLong(int)}
	 * @see {@link #SGM_GET_VERSION}
	 * */
    @Deprecated
	public static final int SGM_GET_VERSION_CODE = 108;

	/**
	 * Get current SDK version, human-readable string will be returned
	 * 
	 * @see {@link GameMaster#getString(int)}
	 * @see {@link #SGM_GET_VERSION_CODE}
	 * */
    @Deprecated
	public static final int SGM_GET_VERSION = 109;

	/**
	 * Get engine state, return 0 when engine has not been initialized,
	 * otherwise return 1
	 * 
	 * @see {@link GameMaster#getLong(int)}
	 */
    @Deprecated
	public static final int SGM_GET_ENGINE_STATE = 110;

	/**
	 * Set the game server port used by this game, if set, GameMaster only focus
	 * on this port, other port will be ignored by the gamemaster
	 * 
	 * @see {@link GameMaster#setLong(int, long)}
	 * */
    @Deprecated
	public static final int SGM_SET_GAME_PORT = 300;

	/**
	 * Set SGM_SET_AUTO_START along with 1, the GameMaster will save the status
	 * of accelerator engine, if player switch on the engine, the engine will
	 * auto start while the game next login.</p> Set SGM_SET_AUTO_START along
	 * with 0, the GameMaster will not auto start the accelerator engine, even
	 * through the player has switched on the engine at last login. The game
	 * should save the engine's on/off status, and perform the auto start action
	 * itself.
	 * */
	@Deprecated
	public static final int SGM_SET_AUTO_START = 301;

	/**
	 * Set log output level of accelerator engine, valid value is 1, 2, 3, 4 and
	 * 5
	 */
    @Deprecated
	public static final int SGM_SET_LOG_LEVEL = 302;

	/**
	 * Set to test mode, valid value is 0 (false) or 1 (true)
	 */
	@Deprecated
	public static final int SGM_SET_TEST_MODE = 303;

	/**
	 * Set a test server node to engine, the engine will use this node instead
	 * of setting by itself
	 * 
	 * @see {@link GameMaster#setString(int, String)}
	 * */
    @Deprecated
	public static final int SGM_SET_TEST_SERVER_NODE = 304;

	/**
	 * Inform GameMaster engine that game has been brought foreground, and
	 * engine will accelerate the game if switch on
	 */
	@Deprecated
	public static final int SGM_SET_GAME_FOREGROUND = 305;

	/**
	 * Inform GameMaster engine that game has been brought background, and
	 * engine will pause for performance sake
	 */
	@Deprecated
	public static final int SGM_SET_GAME_BACKGROUND = 306;

	/**
	 * Set IP list of game server, which the engine will accelerate only.
	 * (<b>White List</b>)
	 */
	@Deprecated
	public static final int SGM_SET_GAME_SERVER_IP = 307;

	/**
	 * Set timeout of TCP connection
	 */
    @Deprecated
	public static final int SGM_SET_CONNECT_TIMEOUT = 308;

}
