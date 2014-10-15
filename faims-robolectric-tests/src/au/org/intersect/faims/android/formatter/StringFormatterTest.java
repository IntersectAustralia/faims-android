package au.org.intersect.faims.android.formatter;

import static org.junit.Assert.*;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StringFormatterTest {
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}
	
	@Test
	public void parseIf() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "test" });
			assertEquals("parsed", "test", (String) createParser("if equal($1,1) then $2").statement().value);
			assertEquals("parsed", "test", (String) createParser("if equal($2,\"test\") then $2").statement().value);
			assertEquals("parsed", "", (String) createParser("if equal($1,2) then $2").statement().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseIfElse() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "test", "foobar" });
			assertEquals("parsed", "foobar", (String) createParser("if equal($1,0) then $2 else $3").statement().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseIfElsifElse() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "test", "foobar" });
			assertEquals("parsed", "test", (String) createParser("if equal($1,0) then $1 elsif $1 then $2 else $3").statement().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseIfElsifElsifElse() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "test", "foobar" });
			assertEquals("parsed", "test", (String) createParser("if equal($1,0) then $1 elsif not($1) then \"what?\" elsif $1 then $2 else $3").statement().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseAnd() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "test" });
			assertEquals("parsed", true, (Boolean) createParser("and(equal($1,1),equal($2,\"test\"))").multi_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("and(equal($1,0),equal($2,\"test\"))").multi_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseOr() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "test" });
			assertEquals("parsed", true, (Boolean) createParser("or(equal($1,1),equal($2,\"test\"))").multi_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("or(equal($1,0),equal($2,\"test\"))").multi_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("or(equal($1,0),equal($2,\"test2\"))").multi_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseAndOr() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "test" });
			assertEquals("parsed", true, (Boolean) createParser("and(equal($1,1), or(equal($1,0),equal($2,\"test\")))").multi_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseEqual() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "test" });
			assertEquals("parsed", true, (Boolean) createParser("equal($1,1)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("equal($1,2)").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("equal($2,\"test\")").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("equal($2,\"foobar\")").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseGreaterThan() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "2" });
			assertEquals("parsed", true, (Boolean) createParser("greaterThan($1,0)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("greaterThan($1,1)").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("greaterThan($2,1)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("greaterThan($2,2)").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseGreaterThanEqual() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "2" });
			assertEquals("parsed", true, (Boolean) createParser("greaterThanEqual($1,1)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("greaterThanEqual($1,2)").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("greaterThanEqual($2,2)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("greaterThanEqual($2,3)").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseLessThan() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "2" });
			assertEquals("parsed", true, (Boolean) createParser("lessThan($1,2)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("lessThan($1,1)").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("lessThan($2,3)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("lessThan($2,2)").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseLessThanEqual() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "2" });
			assertEquals("parsed", true, (Boolean) createParser("lessThanEqual($1,1)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("lessThanEqual($1,0)").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("lessThanEqual($2,2)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("lessThanEqual($2,1)").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseIn() throws Exception {
		try {
			assertEquals("parsed", true, (Boolean) createParser("in(\"test1\", [\"test1\", \"test2\", \"test3\"])").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("in(\"test4\", [\"test1\", \"test2\", \"test3\"])").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseBetween() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "5", "3", "0", "10" });
			assertEquals("parsed", true, (Boolean) createParser("between($1, 1, 5)").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("between($2, 1, 5)").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("between($3, 1, 5)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("between($4, 1, 5)").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("between($5, 1, 5)").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseLiteral() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "2", null });
			assertEquals("parsed", true, (Boolean) createParser("$1").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("$2").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("$3").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseNot() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1", "2", null });
			assertEquals("parsed", true, (Boolean) createParser("$1").single_expression().value);
			assertEquals("parsed", true, (Boolean) createParser("$2").single_expression().value);
			assertEquals("parsed", false, (Boolean) createParser("$3").single_expression().value);
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	@Test
	public void parseInt() throws Exception {
		assertEquals("parsed", "1", (String) createParser("1").literal().value);
		assertEquals("parsed", "-1", (String) createParser("-1").literal().value);
	}
	
	@Test
	public void parseNumber() throws Exception {
		assertEquals("parsed", "1.01", (String) createParser("1.01").literal().value);
		assertEquals("parsed", "-1.01", (String) createParser("-1.01").literal().value);
	}
	
	@Test
	public void parseString() throws Exception {
		assertEquals("parsed", "Hello World", (String) createParser("\"Hello World\"").literal().value);
		assertEquals("parsed", "Hello World", (String) createParser("'Hello World'").literal().value);
	}
	
	@Test
	public void parseVariable() throws Exception {
		try {
			ArgumentMap.setArguments(null, new String[] { "1" });
			assertEquals("variable parsed", (String) createParser("$1").literal().value, "1");
		} finally {
			ArgumentMap.removeArguments(null);
		}
	}
	
	private StatementParser createParser(String statement) throws Exception {
		StatementLexer lexer = new StatementLexer(new ANTLRInputStream(statement));
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		tokens.fill();
		StatementParser parser = new StatementParser(tokens);
		return parser;
	}

}
