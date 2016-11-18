package com.jpmorgan.model;

import org.apache.commons.lang.Validate;

/**
 * Model class that represents a stock.
 * @author bdinos
 */
public enum Stock {
	TEA_C("TEA", Type.COMMON),
	POP_C("POP", Type.COMMON),
	ALE_C("ALE", Type.COMMON),
	GIN_P("GIN", Type.PREFERRED),
	JOE_C("JOE", Type.COMMON),
	;
	
	public final String symbol;
	public final Type type;
	
	private Stock(String symbol, Type type) {
		Validate.notNull(symbol);
		this.symbol = symbol;
		this.type = type;
	}
	
	@Override
	public String toString() {
		return symbol;
	}

	/**
	 * Model class that represents a the type of a stock.
	 * @author bdinos
	 */
	public enum Type {
		COMMON,
		PREFERRED,
		;
	}
}
