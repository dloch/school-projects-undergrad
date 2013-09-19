package com.putable.siteriter.locd011;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.putable.siteriter.SDLParseException;

public enum HighLevelTokens {
	RULE(ruleParser()),
	HEAD(headParser()),
	CHOICE(choiceParser()),
	SEQUENCE(sequenceParser());

	private Parser parser;

	private static boolean complete = false;
	
 	public static Iterable<Symbol> parse(Reader reader) throws IOException {
		List<Symbol> rules = new LinkedList<Symbol>();
		rules.add(RULE.parser.parse(reader));
		Symbol ruleSymbol = rules.get(0);
		if(ruleSymbol == null)
		{
		    complete = false;
		    throw new SDLParseException("Expected RULE, but received nothing");
		}
		while(!complete)
		{
			ruleSymbol = RULE.parser.parse(reader);
			if(ruleSymbol != null)
			    rules.add(ruleSymbol);
		}
		complete = false;
		return rules;
	}

	private static boolean notComplete(Iterable<Tuple<Token, String>> rule) {
	    for (Tuple<Token, String> next : rule)
		complete = complete || next.getFirst() == Token.EOI;
	    return !complete;
	}

	private HighLevelTokens(Parser parser) {
		this.parser = parser;
	}

	private interface Parser {
		public Symbol parse(Reader reader) throws IOException;
	}

	private static Parser ruleParser() {
		return new Parser() {
			public Symbol parse(Reader reader) throws IOException
			{
				Symbol head = HEAD.parser.parse(reader);
				if(head == null)
				    return null;
				Symbol choices = CHOICE.parser.parse(reader);
				Symbol rule = constructRuleSymbol(head, choices);
				return rule;
			}
		};
	}

	private static Parser headParser() {
		return new Parser() {
			@Override
			public Symbol parse(Reader reader) throws IOException
			{
				return constructHead(reader);
			}
		};
	}

	private static Parser choiceParser() {
		return new Parser() {
			@Override
			public Symbol parse(Reader reader) throws IOException
			{
				return constructChoice(reader);
			}
		};
	}

	private static Symbol constructRuleSymbol(Symbol head, Symbol choices)
	{
		final String name = head.getName();
		final boolean isAlternateStart = head.isAlternateStart();
		final String selector = head.getSelector();

		final List<Symbol> choiceList = choices.getChoices();

		final List<Token> tokenList = new ArrayList<Token>();
		// Aggregate the tokens, and add the ones we removed as a byproduct of
		// parsing:
		for (int i = 0; i < head.getTokenLength(); i++)
			tokenList.add(head.getTokenAt(i));
		for (int i = 0; i < choices.getTokenLength(); i++)
			tokenList.add(choices.getTokenAt(i));

		return new Symbol() {
			private List<Symbol> choices = choiceList;
			private List<Token> tokens = tokenList;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public boolean isAlternateStart() {
				return isAlternateStart;
			}

			@Override
			public String getSelector() {
				return selector;
			}

			@Override
			public Symbol getChoiceAt(int index) {
				return choices.get(index);
			}

			@Override
			public Token getTokenAt(int index) {
				return tokens.get(index);
			}

			@Override
			public int getTokenLength() {
				return tokenList.size();
			}

			@Override
			public String evaluate(Random r, Map<String, Symbol> symbolTable, Map<String, Integer> select) {
				if(getSelector() != null)
				{
					if(select.containsKey(getSelector()))
						if(select.get(getSelector()) != null)
							return getChoiceAt(select.get(getSelector()) % choices.size()).evaluate(r, symbolTable, select);
					Integer nextInt = r.nextInt(Integer.MAX_VALUE);
					select.put(getSelector(), nextInt);
					return getChoiceAt(nextInt % choices.size()).evaluate(r, symbolTable, select);
				}
				return choices.get(r.nextInt(choices.size())).evaluate(r, symbolTable, select);
			}

			@Override
			public List<Symbol> getChoices() {
				return choices;
			}
		};
	}

	private static Symbol constructHead(Reader reader) throws IOException {
		// Grammar: [:]NAME[:NAME]
		List<Tuple<Token, String>> tokens = new ArrayList<Tuple<Token, String>>();
		List<Tuple<Token, String>> nextTokens = getNextTokens(reader);
		tokens.addAll(nextTokens);

		if(!notComplete(nextTokens))
			return null;
		int currentToken = 0;
		boolean alternateStartString = nextTokens.get(0).getFirst() == Token.COLON;
		if (alternateStartString) {
			currentToken++;
			if (currentToken == tokens.size()) {
				nextTokens = getNextTokens(reader);
				tokens.addAll(nextTokens);
				currentToken = 0;
			}
		}

		String symbolName;
		if (nextTokens.get(currentToken).getFirst() == Token.NAME) {
			symbolName = nextTokens.get(currentToken).getSecond();
			currentToken++;
		}
		else
			throw new SDLParseException("Expected NAME, but received " + 
					nextTokens.get(currentToken).getFirst());

		if (currentToken == nextTokens.size()) {
			nextTokens = getNextTokens(reader);
			tokens.addAll(nextTokens);
			currentToken = 0;
		}

		String selector = null;
		if (nextTokens.get(currentToken).getFirst() == Token.COLON) {
			currentToken++;
			if (currentToken == nextTokens.size()) {
				nextTokens = getNextTokens(reader);
				tokens.addAll(nextTokens);
				currentToken = 0;
			}
			if (nextTokens.get(currentToken).getFirst() == Token.NAME)
			{
				selector = nextTokens.get(currentToken).getSecond();
				currentToken++;
			}
			else
				throw new SDLParseException("Expected matching NAME for associated COLON!");
		}

		if(currentToken == nextTokens.size())
		{
			nextTokens = getNextTokens(reader);
			tokens.addAll(nextTokens);
			currentToken = 0;
		}
		
		if(nextTokens.get(currentToken).getFirst() == Token.EQUAL)
		{
			return constructHeadSymbol(symbolName, alternateStartString, selector, tokens);
		}
		
		throw new SDLParseException("Expected EQUAL token, but received " + nextTokens.get(currentToken).getFirst());
	}

	private static Symbol constructHeadSymbol(final String name, final boolean alternateStartString, final String selector, final List<Tuple<Token, String>> tokenRepresentation) {
		return new Symbol() {
			private List<Tuple<Token, String>> tokens = tokenRepresentation;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public boolean isAlternateStart() {
				return alternateStartString;
			}

			@Override
			public String getSelector() {
				return selector;
			}

			@Override
			public List<Symbol> getChoices() {
				return null;
			}

			@Override
			public Symbol getChoiceAt(int index) {
				return null;
			}

			@Override
			public Token getTokenAt(int index) {
				return tokens.get(index).getFirst();
			}

			@Override
			public int getTokenLength() {
				return tokens.size();
			}

			@Override
			public String evaluate(Random r,
					Map<String, Symbol> symbolTable,
					Map<String, Integer> select) {
				return null;
			}
		};
	}

	private static Symbol constructChoice(Reader reader) throws IOException
	{
		List<Tuple<Token, String>> nextChoiceTokens = Token.lexUntil(reader, Token.BAR, Token.SEMICOLON);
		List<Token> tokenList = new ArrayList<Token>();
		List<Symbol> choices = new ArrayList<Symbol>();
		while(notComplete(nextChoiceTokens))
		{
			for(Tuple<Token, String> tokenStringPair : nextChoiceTokens)
				tokenList.add(tokenStringPair.getFirst());
			
			if(nextChoiceTokens.size() == 1)
			{
				choices.add(emptySymbol());
				Token lastToken = nextChoiceTokens.get(0).getFirst();
				if(lastToken == Token.SEMICOLON)
				{
				    return constructChoiceSymbol(choices, tokenList);					
				}
			}
			
			else
			{
				Token lastToken = nextChoiceTokens.get(nextChoiceTokens.size() - 1).getFirst();
				if(lastToken == Token.BAR)
					choices.add(constructSequenceSymbol(nextChoiceTokens.subList(0, nextChoiceTokens.size() - 1)));
				else if(lastToken == Token.SEMICOLON)
				{
					choices.add(constructSequenceSymbol(nextChoiceTokens.subList(0, nextChoiceTokens.size() - 1)));
					return constructChoiceSymbol(choices, tokenList);
				}
			}
			nextChoiceTokens = Token.lexUntil(reader, Token.BAR, Token.SEMICOLON);
		}
		throw new SDLParseException("SEMICOLON expected, but found EOI!");
	}
	
	private static Symbol constructChoiceSymbol(List<Symbol> choices, List<Token> tokens)
	{
	    final List<Symbol> choiceList = choices;
	    final List<Token> tokenList = tokens; 
		return new Symbol()
		{
		    List<Symbol> choices = choiceList;
		    List<Token> tokens = tokenList;
		    
			@Override
			public String getName()
			{
				return null;
			}

			@Override
			public boolean isAlternateStart()
			{
				return false;
			}

			@Override
			public String getSelector()
			{
				return null;
			}

			@Override
			public List<Symbol> getChoices()
			{
				return choices;
			}

			@Override
			public Symbol getChoiceAt(int index)
			{
				return choices.get(index);
			}

			@Override
			public Token getTokenAt(int index)
			{
				return tokens.get(index);
			}

			@Override
			public int getTokenLength()
			{
				return tokens.size();
			}

			@Override
			public String evaluate(Random r, Map<String, Symbol> symbolTable, Map<String, Integer> select)
			{
				return null;
			}
		};
	}
	
	private static List<Tuple<Token, String>> getNextTokens(Reader reader)
			throws IOException {
		List<Tuple<Token, String>> nextTokens = Token.getNextTokens(reader);
		while (nextTokens.size() == 0)
			nextTokens = Token.getNextTokens(reader);
		return nextTokens;
	}

	private static Parser sequenceParser() {
		return new Parser() {
			@Override
			public Symbol parse(Reader reader) {
				// Iterable<Tuple<LazyToken, String>> choice =
				// LazyToken.lexUntil(reader, LazyToken.BAR,
				// LazyToken.SEMICOLON);
				return null;
			}
		};
	}

	private static Symbol constructSequenceSymbol(List<Tuple<Token, String>> input) throws IOException
	{
		for(Tuple<Token, String> next : input)
		{
			Token type = next.getFirst();
			boolean isValid = type == Token.DLITERAL || type == Token.SLITERAL
					|| type == Token.NAME;
			if(!isValid)
				throw new SDLParseException("Expected DLITERAL, SLITERAL, or NAME. Received " + type);
		}
		
		final List<Tuple<Token, String>> sequenceValues = input;
		return new Symbol() {
			private List<Tuple<Token, String>> tokens = sequenceValues;

			public int getTokenLength() {
				return tokens.size();
			}

			@Override
			public String getName() {
				return "SEQUENCE";
			}

			@Override
			public Symbol getChoiceAt(int index) {
				return null;
			}

			@Override
			public Token getTokenAt(int index) {
				return tokens.get(index).getFirst();
			}

			@Override
			public String evaluate(Random r, Map<String, Symbol> symbolTable, Map<String, Integer> select) {
				String output = "";
				for (Tuple<Token, String> nextToken : sequenceValues) {
					if (nextToken.getFirst() == Token.NAME) {
						if (symbolTable.containsKey(nextToken.getSecond()))
							output += symbolTable.get(nextToken.getSecond())
									.evaluate(r, symbolTable, select);
						else
							output += nextToken.getSecond() + '?';
					} else {
						String value = nextToken.getSecond();
						output += value.substring(1, value.length() - 1);
					}
				}
				return output;
			}

			@Override
			public List<Symbol> getChoices() {
				return null;
			}

			@Override
			public boolean isAlternateStart() {
				return false;
			}

			@Override
			public String getSelector() {
				return null;
			}
		};
	}
	
	private static Symbol emptySymbol()
	{
		return new Symbol()
		{

			@Override
			public String getName()
			{
				return null;
			}

			@Override
			public boolean isAlternateStart()
			{
				return false;
			}

			@Override
			public String getSelector()
			{
				return null;
			}

			@Override
			public List<Symbol> getChoices()
			{
				return null;
			}

			@Override
			public Symbol getChoiceAt(int index)
			{
				return null;
			}

			@Override
			public Token getTokenAt(int index)
			{
				return null;
			}

			@Override
			public int getTokenLength()
			{
				return 0;
			}

			@Override
			public String evaluate(Random r, Map<String, Symbol> symbolTable, Map<String, Integer> select)
			{
				return "";
			}
			
		};
	}
}