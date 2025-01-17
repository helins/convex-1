package convex.core;

/**
 * Static Constants for Coin sizes and total supply
 */
public class Coin {
	/**
	 * Copper coin, the lowest (indivisible) denomination.
	 */
	public static final long COPPER=1L;
	
	/**
	 * Copper coin, a denomination for small change/ Equal to 1000 Copper
	 */
	public static final long BRONZE=1000*COPPER;
	
	/**
	 * Silver Coin, a denomination for small payments. Equal to 1000 Bronze
	 */
	public static final long SILVER=1000*BRONZE;
	
	/**
	 * A denomination suitable for medium/large payments. Equal to 1000 Silver, and divisible into one billion copper coins.
	 * 
	 * Intended as the primary "human scale" quanity of Convex Coins in regular usage.
	 */
	public static final long GOLD=1000*SILVER;
	
	/**
	 * A large denomination. 1000 Gold.
	 */
	public static final long DIAMOND=1000*GOLD;
	
	/**
	 * A massively valuable amount of Convex Coins. One million Gold.
	 */
	public static final long EMERALD=1000*DIAMOND;
	
	/**
	 * The total Convex Coin maximum supply limit. One billion Gold Coins
	 */
	public static final long SUPPLY=1000*EMERALD;
}
