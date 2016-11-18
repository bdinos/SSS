package com.jpmorgan;

import static org.junit.Assert.assertEquals;

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
	StockExchange stockExchange;
	
	@Before
	public void setup() {
		stockExchange = new TestStockExchange();
	}
	
	@Test(expected=DuplicateTickerException.class)
	public void duplicateTickerTest() throws DuplicateTickerException {
		stockExchange.registerTicker(new Ticker(Stock.TEA_C, 1, 1.0));
	}
	
	@Test
	public void latestTradesTest() throws TickerNotFoundException {
		InstantPacer pacer = new InstantPacer();
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(30), Stock.ALE_C, 1, 1.0));
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(60 + 60 + 30), Stock.TEA_C, 1, 1.0));
		
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
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, 1.0));
		assertEquals(1.0, stockExchange.getStockPrice(theStock), 0.1);
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), Stock.TEA_C, 1, 10.0)); //this should be filtered out
		assertEquals(1.0, stockExchange.getStockPrice(theStock), 0.1);
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, 4.0));
		assertEquals(2.5, stockExchange.getStockPrice(theStock), 0.1);
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 4, 4.0));
		assertEquals(3.5, stockExchange.getStockPrice(theStock), 0.1);
	}
	
	@Test(expected=TickerPriceNotAvailableException.class)
	public void dividendYieldNoTradesTest() throws TickerNotFoundException, TickerPriceNotAvailableException {
		stockExchange.getStockDividendYield(Stock.ALE_C);
	}
	
	@Test
	public void dividendYieldCommonStocksTest() throws TickerNotFoundException, TickerPriceNotAvailableException {
		InstantPacer pacer = new InstantPacer();
		Stock theStock = Stock.POP_C;
		double tickerPrice = 20.0;
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, tickerPrice));
		assertEquals(TestStockExchange.POP_C_LAST_DIVIDEND / tickerPrice, stockExchange.getStockDividendYield(theStock), 0.1);
	}
	
	@Test
	public void dividendYieldPreferredStocksTest() throws TickerNotFoundException, TickerPriceNotAvailableException {
		InstantPacer pacer = new InstantPacer();
		Stock theStock = Stock.GIN_P;
		double tickerPrice = 20.0;
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, tickerPrice));
		assertEquals(TestStockExchange.GIN_P_FIXED_DIVIDEND * TestStockExchange.GIN_P_PAR_VALUE / tickerPrice, stockExchange.getStockDividendYield(theStock), 0.1);
	}
	
	@Test
	public void priceEarningsRatioTest() throws TickerNotFoundException, EPSNotAvailableException {
		InstantPacer pacer = new InstantPacer();
		Stock theStock = Stock.POP_C;
		double tickerPrice = 20.0;
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), theStock, 1, tickerPrice));
		assertEquals(tickerPrice / TestStockExchange.POP_C_LAST_DIVIDEND, 
				stockExchange.getStockPriceEarningsRatio(theStock), 0.1);
	}
	
	@Test
	public void allShareIndexTest() throws TickerNotFoundException, NotEnoughDataPointsException {
		InstantPacer pacer = new InstantPacer();
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), Stock.TEA_C, 1, 10.0));
		stockExchange.recordTrade(Trade.sell(pacer.getInstantAndIncrementBySeconds(1), Stock.TEA_C, 3, 8.0));
		
		stockExchange.recordTrade(Trade.buy(pacer.getInstantAndIncrementBySeconds(1), Stock.ALE_C, 6, 5.0));
		stockExchange.recordTrade(Trade.sell(pacer.getInstantAndIncrementBySeconds(1), Stock.ALE_C, 2, 3.0));
		
		double teaPrice = stockExchange.getStockPrice(Stock.TEA_C);
		double alePrice = stockExchange.getStockPrice(Stock.ALE_C);
		
		double idx = Math.sqrt(teaPrice * alePrice);
		
		assertEquals(idx, stockExchange.getGBCEAllShareIndex(), 0.1);
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
		private static final double POP_C_LAST_DIVIDEND = 8.0;
		private static final double GIN_P_FIXED_DIVIDEND = 2.0 / 100.0;
		private static final double GIN_P_PAR_VALUE = 100.0;

		public TestStockExchange() {
			super();
			setup();
		}

		private void setup() {
			try {
				this.registerTicker(new Ticker(Stock.TEA_C, 0.0, 100.0));
				this.registerTicker(new Ticker(Stock.POP_C, POP_C_LAST_DIVIDEND, 100.0));
				this.registerTicker(new Ticker(Stock.ALE_C, 23.0, 60.0));
				this.registerTicker(new Ticker(Stock.GIN_P, 8.0, GIN_P_FIXED_DIVIDEND, GIN_P_PAR_VALUE));
				this.registerTicker(new Ticker(Stock.JOE_C, 13.0, 250.0));
			} catch (DuplicateTickerException e) {
				throw new RuntimeException(e); //not expected
			}
		}
	}
}
