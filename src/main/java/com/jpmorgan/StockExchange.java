package com.jpmorgan;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.Validate;
import org.apache.commons.math3.stat.StatUtils;

import com.jpmorgan.model.Stock;
import com.jpmorgan.model.Ticker;
import com.jpmorgan.model.Ticker.EPSNotAvailableException;
import com.jpmorgan.model.Ticker.TickerPriceNotAvailableException;
import com.jpmorgan.model.Trade;

/**
 * This is the main class of the Super Simple Stocks application. It contains methods for registering stocks,
 * recording trades and get the relevant information about a stock.
 * Stocks should be registered to the market by using the {@link StockExchange#registerTicker(Ticker)} method.
 * Trades should be recorded by using the {@link StockExchange#recordTrade(Trade)} method.
 * @author bdinos
 */
public class StockExchange {
	public static final MathContext MATH_CTX = new MathContext(2, RoundingMode.HALF_UP);
	
	private final HashMap<Stock, Ticker> tickersByStock = new HashMap<>();
	
	private final TreeSet<Trade> trades = new TreeSet<>( (t1, t2) -> {
		return t1.timestamp.compareTo(t2.timestamp);
	});
	
	protected Set<Trade> getLatestTrades(Instant timestamp, int minutes) {
		Validate.isTrue(minutes > 0);
		Instant before = timestamp.minus(minutes, ChronoUnit.MINUTES);
		return trades.stream().filter( trade -> trade.timestamp.isAfter(before) ).collect(Collectors.toSet());
	}
	
	protected  Set<Trade> getLatestTrades(int minutes) {
		return getLatestTrades(Instant.now(), minutes);
	}
	
	/**
  	 * Calculate the GBCE All Share Index, as the geometric mean of all stock prices.
  	 * @return the GBCE All Share Index
  	 */
	public BigDecimal getGBCEAllShareIndex() {
		Set<Trade> latestTrades = getLatestTrades(15);
		Collection<BigDecimal> prices = new ArrayList<>();
		for(Ticker ticker: tickersByStock.values()) {
			try {
				BigDecimal price = getStockPrice(ticker.stock, latestTrades);
				prices.add(price);
			} catch (NotEnoughDataPointsException e) {
				//do nothing
			}
		}
		double[] pricesArr = prices.stream().mapToDouble(i-> i.doubleValue()).toArray();
		return BigDecimal.valueOf(StatUtils.geometricMean(pricesArr));
	}
	
	/**
  	 * Calculate the price of a stock. The price is calculated by performing the weighted average of the stock 
  	 * trades done in the latest 15 minutes. 
  	 * @param stock
  	 * @return the stock's price
  	 * @throws NotEnoughDataPointsException if not enough trade data is available in order to perform the calculation
  	 */
	public BigDecimal getStockPrice(Stock stock) throws NotEnoughDataPointsException {
		Set<Trade> latestTrades = getLatestTrades(15);
		return getStockPrice(stock, latestTrades);
	}
	
	/**
  	 * Calculate the dividend yield of a stock.
  	 * @param stock
  	 * @return the stock's dividend yield
  	 * @throws TickerNotFoundException if the stock has not a ticker associated
  	 * @throws TickerPriceNotAvailableException if the stock has not been marketed yet
  	 */
	public BigDecimal getStockDividendYield(Stock stock) throws TickerNotFoundException, TickerPriceNotAvailableException {
		Ticker ticker = tickersByStock.get(stock);
		if(ticker == null) {
			throw new TickerNotFoundException(stock);
		}
		return ticker.getDividendYield();
	}
	
	/**
  	 * Calculate the dividend yield of a stock as a percentage (%).
  	 * @param stock
  	 * @return the stock's dividend yield as a percentage (%)
  	 * @throws TickerNotFoundException if the stock has not a ticker associated
  	 * @throws TickerPriceNotAvailableException if the stock has not been marketed yet
  	 */
	public BigDecimal getStockDividendYieldAsPerc(Stock stock) throws TickerNotFoundException, TickerPriceNotAvailableException{
		return getStockDividendYield(stock).multiply(BigDecimal.valueOf(100.0));
	}
	
	/**
  	 * Calculate the PE ratio of a stock.
  	 * @param stock
  	 * @return the stock's PE ratio
  	 * @throws TickerNotFoundException if the stock has not a ticker associated
  	 * @throws EPSNotAvailableException if no data is available to calculate the EPS
  	 */
	public BigDecimal getStockPriceEarningsRatio(Stock stock) throws TickerNotFoundException, EPSNotAvailableException {
		Ticker ticker = tickersByStock.get(stock);
		if(ticker == null) {
			throw new TickerNotFoundException(stock);
		}
		return ticker.getPriceEarningsRatio();
	}
	
	private static class Accumulator {
		BigDecimal amount = BigDecimal.ZERO;
        long count;
        
		public BigDecimal getWeightedAverage() throws NotEnoughDataPointsException {
			if(count > 0) {
				return amount.divide(BigDecimal.valueOf(count), MATH_CTX);
			}
			throw new NotEnoughDataPointsException();
		}
		
		Accumulator accumulate(Trade trade) {
			amount = amount.add(BigDecimal.valueOf(trade.sharesQuantity).multiply(trade.price, MATH_CTX));
			count += trade.sharesQuantity;
			return this;
		}
		
		void combine(Accumulator box) {
			amount = amount.add(box.amount);
			count += box.count;
		}
    }
	
	private BigDecimal getStockPrice(Stock stock, Set<Trade> latestTrades) throws NotEnoughDataPointsException {
		Stream<Trade> tradeStream = latestTrades.stream().filter( trade -> trade.stock == stock );
		return tradeStream.collect(Accumulator::new, Accumulator::accumulate, Accumulator::combine).getWeightedAverage();
	}
	
	/**
  	 * Register a stock into the market by providing its initial ticker @see {@link Ticker}.
  	 * @param ticker the stock's ticker
  	 * @throws DuplicateTickerException if a ticker for the given stock has been already registered
  	 */
	public void registerTicker(Ticker ticker) throws DuplicateTickerException {
		if(tickersByStock.containsKey(ticker.stock)) {
			throw new DuplicateTickerException(ticker.stock);
		}
		tickersByStock.put(ticker.stock, ticker);
	}
	
	/**
  	 * Record a trade to the market @see {@link Trade}
  	 * @param trade the trade to be recorded
  	 * @throws TickerNotFoundException if the stock has not a ticker associated
  	 */
	public void recordTrade(Trade trade) throws TickerNotFoundException {
		Ticker ticker = tickersByStock.get(trade.stock);
		if(ticker == null) {
			throw new TickerNotFoundException(trade.stock);
		}
		ticker.setTickerPrice(trade.price);
		trades.add(trade);
	}
	
	public static class NotEnoughDataPointsException extends Exception {
		private static final long serialVersionUID = -639537271575097279L;
	}
	
	public static class TickerNotFoundException extends Exception {
		private static final long serialVersionUID = 3295016287272659323L;
		
		public TickerNotFoundException(Stock stock) {
			super(String.format("Ticker %s has not been registered", stock));
		}
	}
	
	public static class DuplicateTickerException extends Exception {
		private static final long serialVersionUID = 3295016287272659323L;

		public DuplicateTickerException(Stock stock) {
			super(String.format("Ticker %s has already been registered", stock));
		}
	}
}
