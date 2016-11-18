package com.jpmorgan.model;

import java.time.Instant;

import org.apache.commons.lang.Validate;

/**
 * Model class that represents a trade.
 * @author bdinos
 */
public class Trade {
	public final Instant timestamp;
	public final Stock stock;
	public final TradeIndicator tradeIndicator;
	public final int sharesQuantity;
	public final double price;
	
	private Trade(Instant timestamp, Stock stock, TradeIndicator tradeIndicator, int sharesQuantity, double price) {
		Validate.notNull(timestamp);
		this.timestamp = timestamp;
		Validate.notNull(stock);
		this.stock = stock;
		Validate.notNull(tradeIndicator);
		this.tradeIndicator = tradeIndicator;
		Validate.isTrue(sharesQuantity > 0);
		this.sharesQuantity = sharesQuantity;
		Validate.isTrue(price > 0.0);
		this.price = price;
	}
	
	private Trade(Stock stock, TradeIndicator tradeIndicator, int sharesQuantity, double price) { 
		this(Instant.now(), stock, tradeIndicator, sharesQuantity, price);
	}

	/**
  	 * Create a buy trade object having <code>Instant.now()</code> as the time stamp.
  	 * @param stock
  	 * @param sharesQuantity the number of shares exchanged
  	 * @param price the price of the shares
  	 * @return the trade object
  	 */
	public static Trade buy(Stock stock, int sharesQuantity, double price) {
		return new Trade(stock, TradeIndicator.BUY, sharesQuantity, price);
	}
	
	/**
  	 * Create a sell trade object having <code>Instant.now()</code> as the time stamp.
  	 * @param stock
  	 * @param sharesQuantity the number of shares exchanged
  	 * @param price the price of the shares
  	 * @return the trade object
  	 */
	public static Trade sell(Stock stock, int sharesQuantity, double price) {
		return new Trade(stock, TradeIndicator.SELL, sharesQuantity, price);
	}
	
	/**
  	 * Create a buy trade object having <code>Instant.now()</code> as the time stamp.
  	 * @param the instant when the trade was performed
  	 * @param stock
  	 * @param sharesQuantity the number of shares exchanged
  	 * @param price the price of the shares
  	 * @return the trade object
  	 */
	public static Trade buy(Instant timestamp, Stock stock, int sharesQuantity, double price) {
		return new Trade(timestamp, stock, TradeIndicator.BUY, sharesQuantity, price);
	}
	
	/**
  	 * Create a sell trade object having <code>Instant.now()</code> as the time stamp.
  	 * @param the instant when the trade was performed
  	 * @param stock
  	 * @param sharesQuantity the number of shares exchanged
  	 * @param price the price of the shares
  	 * @return the trade object
  	 */
	public static Trade sell(Instant timestamp, Stock stock, int sharesQuantity, double price) {
		return new Trade(timestamp, stock, TradeIndicator.SELL, sharesQuantity, price);
	}

	/**
	 * Model class that represents a trade indicator (BUY or SELL).
	 * @author bdinos
	 */
	public enum TradeIndicator {
		BUY,
		SELL,
		;
	}
}
