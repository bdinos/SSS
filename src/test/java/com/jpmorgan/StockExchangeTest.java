package com.jpmorgan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import com.jpmorgan.StockExchange.DuplicateTickerException;
import com.jpmorgan.StockExchange.NotEnoughDataPointsException;
import com.jpmorgan.StockExchange.TickerNotFoundException;
import com.jpmorgan.model.Stock;
import com.jpmorgan.model.Ticker;
import com.jpmorgan.model.Ticker.EPSNotAvailableException;
import com.jpmorgan.model.Ticker.TickerPriceNotAvailableException;
import com.jpmorgan.model.Trade;

public class StockExchangeTest {
	private final static MathContext MATH_CTX = StockExchange.MATH_CTX;
	
	StockExchange stockExchange;
	
	@Before
	public void setup() {
		stockExchange = new TestStockExchange();
	}
	
	@Test(expected=DuplicateTickerException.class)
	public void duplicateTickerTest() throws DuplicateTickerException {
		stockExchange.registerTicker(new Ticker(Stock.TEA_C, BigDecimal.ONE, BigDecimal.ONE));
	}
	
	@Test
	public void latestTradesTest() throws TickerNotFoundException {
		InstantPacer pacer = new InstantPacer();
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(30), Stock.ALE_C, 1, BigDecimal.ONE));
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(60 + 60 + 30), Stock.TEA_C, 1, BigDecimal.ONE));
		
		Instant current = pacer.getInstant();
		assertEquals(0, stockExchange.getLatestTrades(current, 1).size());
		assertEquals(0, stockExchange.getLatestTrades(current, 2).size());
		assertEquals(1, stockExchange.getLatestTrades(current, 3).size());
		assertEquals(Stock.TEA_C, stockExchange.getLatestTrades(current, 3).iterator().next().stock);
		assertEquals(2, stockExchange.getLatestTrades(current, 4).size());
		assertEquals(2, stockExchange.getLatestTrades(current, 5).size());
	}
	
	@Test(expected=NotEnoughDataPointsException.class)
	public void stockPriceExceptionTest() throws NotEnoughDataPointsException {
		stockExchange.getStockPrice(Stock.ALE_C);
	}
	
	@Test
	public void stockPriceTest() throws NotEnoughDataPointsException, TickerNotFoundException {
		InstantPacer pacer = new InstantPacer();
		Stock theStock = Stock.ALE_C;
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, BigDecimal.ONE));
		assertTrue(stockExchange.getStockPrice(theStock).compareTo(BigDecimal.ONE) == 0);
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), Stock.TEA_C, 1, BigDecimal.valueOf(10))); //this should be filtered out
		assertTrue(stockExchange.getStockPrice(theStock).compareTo(BigDecimal.ONE) == 0);
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, BigDecimal.valueOf(4.0)));
		assertTrue(stockExchange.getStockPrice(theStock).compareTo(BigDecimal.valueOf(2.5)) == 0);
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 4, BigDecimal.valueOf(4.0)));
		assertTrue(stockExchange.getStockPrice(theStock).compareTo(BigDecimal.valueOf(3.5)) == 0);
	}
	
	@Test(expected=TickerPriceNotAvailableException.class)
	public void dividendYieldNoTradesTest() throws TickerNotFoundException, TickerPriceNotAvailableException {
		stockExchange.getStockDividendYield(Stock.ALE_C);
	}
	
	@Test
	public void dividendYieldCommonStocksTest() throws TickerNotFoundException, TickerPriceNotAvailableException {
		InstantPacer pacer = new InstantPacer();
		Stock theStock = Stock.POP_C;
		BigDecimal tickerPrice = BigDecimal.valueOf(20.0);
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, tickerPrice));
		assertTrue(TestStockExchange.POP_C_LAST_DIVIDEND.
				divide(tickerPrice, MATH_CTX).
				compareTo(stockExchange.getStockDividendYield(theStock)) == 0);
	}
	
	@Test
	public void dividendYieldPreferredStocksTest() throws TickerNotFoundException, TickerPriceNotAvailableException {
		InstantPacer pacer = new InstantPacer();
		Stock theStock = Stock.GIN_P;
		BigDecimal tickerPrice = BigDecimal.valueOf(20.0);
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, tickerPrice));
		assertTrue(TestStockExchange.GIN_P_FIXED_DIVIDEND.multiply(TestStockExchange.GIN_P_PAR_VALUE, MATH_CTX).
				divide(tickerPrice, MATH_CTX).
				compareTo(stockExchange.getStockDividendYield(theStock)) == 0);
	}
	
	@Test
	public void priceEarningsRatioTest() throws TickerNotFoundException, EPSNotAvailableException {
		InstantPacer pacer = new InstantPacer();
		Stock theStock = Stock.POP_C;
		BigDecimal tickerPrice = BigDecimal.valueOf(20.0);
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, tickerPrice));
		assertTrue(tickerPrice.divide(TestStockExchange.POP_C_LAST_DIVIDEND, MATH_CTX)
				.compareTo(stockExchange.getStockPriceEarningsRatio(theStock)) == 0);
	}
	
	@Test
	public void allShareIndexTest() throws TickerNotFoundException, NotEnoughDataPointsException {
		InstantPacer pacer = new InstantPacer();
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), Stock.TEA_C, 1, BigDecimal.valueOf(10.0)));
		stockExchange.recordTrade(Trade.sell(pacer.getInstantAndIncrementBySeconds(1), Stock.TEA_C, 3, BigDecimal.valueOf(8.0)));
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), Stock.ALE_C, 6, BigDecimal.valueOf(5.0)));
		stockExchange.recordTrade(Trade.sell(pacer.getInstantAndIncrementBySeconds(1), Stock.ALE_C, 2, BigDecimal.valueOf(3.0)));
		
		BigDecimal teaPrice = stockExchange.getStockPrice(Stock.TEA_C);
		BigDecimal alePrice = stockExchange.getStockPrice(Stock.ALE_C);
		
		double idx = Math.sqrt(teaPrice.multiply(alePrice, MATH_CTX).doubleValue());
		
		System.out.println("" + idx);
		System.out.println("" + stockExchange.getGBCEAllShareIndex());
		System.out.println("" + BigDecimal.valueOf(idx).compareTo(stockExchange.getGBCEAllShareIndex()));
		
		assertTrue(BigDecimal.valueOf(idx).round(MATH_CTX)
				.compareTo(stockExchange.getGBCEAllShareIndex().round(MATH_CTX)) == 0);
	}
	
	private static class InstantPacer {
		private Instant current = Instant.now();
		
		public Instant getInstantAndIncrementBySeconds(int seconds) {
			Instant rer = current;
			current = current.plus(seconds, ChronoUnit.SECONDS);
			return rer;
		}

		public Instant getInstant() {
			return current;
		}
	}
	
	private static class TestStockExchange extends StockExchange {
		private static final BigDecimal POP_C_LAST_DIVIDEND = BigDecimal.valueOf(8.0);
		private static final BigDecimal GIN_P_FIXED_DIVIDEND = BigDecimal.valueOf(2.0 / 100.0);
		private static final BigDecimal GIN_P_PAR_VALUE = BigDecimal.valueOf(100.0);

		public TestStockExchange() {
			super();
			setup();
		}

		private void setup() {
			try {
				this.registerTicker(new Ticker(Stock.TEA_C, BigDecimal.ZERO, BigDecimal.valueOf(100.0)));
				this.registerTicker(new Ticker(Stock.POP_C, POP_C_LAST_DIVIDEND, BigDecimal.valueOf(100.0)));
				this.registerTicker(new Ticker(Stock.ALE_C, BigDecimal.valueOf(23.0), BigDecimal.valueOf(60.0)));
				this.registerTicker(new Ticker(Stock.GIN_P, BigDecimal.valueOf(8.0), GIN_P_FIXED_DIVIDEND, GIN_P_PAR_VALUE));
				this.registerTicker(new Ticker(Stock.JOE_C, BigDecimal.valueOf(13.0), BigDecimal.valueOf(250.0)));
			} catch (DuplicateTickerException e) {
				throw new RuntimeException(e); //not expected
			}
		}
	}
}
