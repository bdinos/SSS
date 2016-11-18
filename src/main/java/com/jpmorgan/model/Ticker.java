package com.jpmorgan.model;

import java.math.BigDecimal;
import java.math.MathContext;

import org.apache.commons.lang.Validate;

import com.jpmorgan.StockExchange;

/**
 * Model class that represents a stock's ticker.
 * @author bdinos
 */
public class Ticker {
	private final static MathContext MATH_CTX = StockExchange.MATH_CTX;
	public final Stock stock;
	private BigDecimal lastDividend;
	private BigDecimal fixedDividend; //is a fraction
	private BigDecimal parValue;
	private BigDecimal tickerPrice = BigDecimal.ZERO;

	/**
  	 * Constructor.
  	 * @param stock
  	 * @param lastDividend the stock's last dividend
  	 * @param parValue the stock's par value
  	 */
	public Ticker(Stock stock, BigDecimal lastDividend, BigDecimal parValue) {
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
	public Ticker(Stock stock, BigDecimal lastDividend, BigDecimal fixedDividend, BigDecimal parValue) {
		this(stock, lastDividend, parValue);
		setFixedDividend(fixedDividend);
	}
	
	/**
  	 * Calculate the PE ratio of this ticker.
  	 * @return the PE ratio of this ticker
  	 */
	public BigDecimal getPriceEarningsRatio() throws EPSNotAvailableException {		
		BigDecimal eps = getEarningsPerShare();
		if(eps.signum() > 0) {
			return tickerPrice.divide(eps, MATH_CTX);	
		}
		throw new EPSNotAvailableException();
	}
	
	/**
  	 * Calculate the EPS of this ticker.
  	 * @return the EPS of this ticker
  	 */
	private BigDecimal getEarningsPerShare() {
		// we approximate it roughly to lastDividend
		return lastDividend;
	}

	private BigDecimal getDividend() {
		BigDecimal dividend;
		if(stock.type == Stock.Type.COMMON){
			dividend = lastDividend;
		} else {
			dividend = fixedDividend.multiply(parValue, MATH_CTX);
		}
		return dividend;
	}
	
	/**
  	 * Calculate the dividend yield of this ticker.
  	 * @return the dividend yield of this ticker
  	 */
	public BigDecimal getDividendYield() throws TickerPriceNotAvailableException {
		if(tickerPrice.signum() <= 0){
			throw new TickerPriceNotAvailableException();
		}
		return getDividend().divide(tickerPrice, MATH_CTX);
	}

	public BigDecimal getLastDividend() {
		return lastDividend;
	}

	public void setLastDividend(BigDecimal lastDividend) {
		Validate.isTrue(lastDividend.signum() >= 0);
		this.lastDividend = lastDividend;
	}

	public BigDecimal getFixedDividend() {
		return fixedDividend;
	}

	public void setFixedDividend(BigDecimal fixedDividend) {
		Validate.isTrue(fixedDividend.signum() > 0);
		this.fixedDividend = fixedDividend;
	}

	public BigDecimal getParValue() {
		return parValue;
	}

	public void setParValue(BigDecimal parValue) {
		Validate.isTrue(parValue.signum() > 0);
		this.parValue = parValue;
	}

	public BigDecimal getTickerPrice() {
		return tickerPrice;
	}

	public void setTickerPrice(BigDecimal tickerPrice) {
		Validate.isTrue(tickerPrice.signum() > 0);
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
