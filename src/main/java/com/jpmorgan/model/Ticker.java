package com.jpmorgan.model;

import org.apache.commons.lang.Validate;

/**
 * Model class that represents a stock's ticker.
 * @author bdinos
 */
public class Ticker {
	public final Stock stock;
	private double lastDividend;
	private double fixedDividend; //is a fraction
	private double parValue;
	private double tickerPrice;

	/**
  	 * Constructor.
  	 * @param stock
  	 * @param lastDividend the stock's last dividend
  	 * @param parValue the stock's par value
  	 */
	public Ticker(Stock stock, double lastDividend, double parValue) {
		this.stock = stock;
		setLastDividend(lastDividend);
		setParValue(parValue);
	}
	
	/**
  	 * Constructor.
  	 * @param stock
  	 * @param lastDividend the stock's last dividend
  	 * @param fixedDividend the stock's fixed dividend
  	 * @param parValue the stock's par value
  	 */
	public Ticker(Stock stock, double lastDividend, double fixedDividend, double parValue) {
		this(stock, lastDividend, parValue);
		setFixedDividend(fixedDividend);
	}
	
	/**
  	 * Calculate the PE ratio of this ticker.
  	 * @return the PE ratio of this ticker
  	 */
	public double getPriceEarningsRatio() throws EPSNotAvailableException {		
		double eps = getEarningsPerShare();
		if(eps > 0.0) {
			return tickerPrice / eps;	
		}
		throw new EPSNotAvailableException();
	}
	
	/**
  	 * Calculate the EPS of this ticker.
  	 * @return the EPS of this ticker
  	 */
	private double getEarningsPerShare() {
		// we approximate it roughly to lastDividend
		return lastDividend;
	}

	private double getDividend() {
		double dividend;
		if(stock.type == Stock.Type.COMMON){
			dividend = lastDividend;
		} else {
			dividend = fixedDividend * parValue;
		}
		return dividend;
	}
	
	/**
  	 * Calculate the dividend yield of this ticker.
  	 * @return the dividend yield of this ticker
  	 */
	public double getDividendYield() throws TickerPriceNotAvailableException {
		if(tickerPrice <= 0.0){
			throw new TickerPriceNotAvailableException();
		}
		return getDividend() / tickerPrice;
	}

	public double getLastDividend() {
		return lastDividend;
	}

	public void setLastDividend(double lastDividend) {
		Validate.isTrue(lastDividend >= 0.0);
		this.lastDividend = lastDividend;
	}

	public double getFixedDividend() {
		return fixedDividend;
	}

	public void setFixedDividend(double fixedDividend) {
		Validate.isTrue(fixedDividend > 0.0);
		this.fixedDividend = fixedDividend;
	}

	public double getParValue() {
		return parValue;
	}

	public void setParValue(double parValue) {
		Validate.isTrue(parValue > 0.0);
		this.parValue = parValue;
	}

	public double getTickerPrice() {
		return tickerPrice;
	}

	public void setTickerPrice(double tickerPrice) {
		Validate.isTrue(tickerPrice > 0.0);
		this.tickerPrice = tickerPrice;
	}
	
	public class TickerPriceNotAvailableException extends Exception {
		private static final long serialVersionUID = -4082993749160766505L;

		public TickerPriceNotAvailableException() {
			super(String.format("The ticker price of %s is not available", stock));
		}
	}
	
	public class EPSNotAvailableException extends Exception {
		private static final long serialVersionUID = -5749778049866651895L;
	}
}
