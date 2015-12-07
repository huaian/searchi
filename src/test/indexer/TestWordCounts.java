package test.indexer;

import indexer.WordCounts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

public class TestWordCounts extends TestCase {

	@Test
	public void testCounts() {
		List<String> input = Arrays.asList("a", "b", "c", "a", "b", "a", "c",
				"d", "d", "d", "a");
		Map<String, Integer> expected = new HashMap<String, Integer>();
		expected.put("a", 4);
		expected.put("b", 2);
		expected.put("c", 2);
		expected.put("d", 3);

		WordCounts actual = new WordCounts(input);
		
		assertEquals(expected, actual.getCounts());
		assertEquals("a", actual.getMaxWord());
		
		assertEquals(new HashSet<>(Arrays.asList(1,4,6,11)), actual.getPosition("a"));
		assertEquals(new HashSet<>(Arrays.asList(2,5)), actual.getPosition("b"));
		assertEquals(new HashSet<>(Arrays.asList(3,7)), actual.getPosition("c"));
		assertEquals(new HashSet<>(Arrays.asList(8,9,10)), actual.getPosition("d"));

		double alpha = 0.5;
		Map<String, Double> maxFreqs = new HashMap<String, Double>();
		maxFreqs.put("a", alpha + (1 - alpha) * 1);
		maxFreqs.put("b", alpha + (1 - alpha) * 2 / 4);
		maxFreqs.put("c", alpha + (1 - alpha) * 2 / 4);
		maxFreqs.put("d", alpha + (1 - alpha) * 3 / 4);

		for (String word : actual)
			assertEquals(maxFreqs.get(word),
					actual.getMaximumTermFrequency(word));

		double docSize = Math.sqrt(sqr(expected.get("a"))
				+ sqr(expected.get("b")) + sqr(expected.get("c"))
				+ sqr(expected.get("d")));
		
		Map<String, Double> euclidFreqs = new HashMap<String, Double>();
		euclidFreqs.put("a", expected.get("a") / docSize);
		euclidFreqs.put("b", expected.get("b") / docSize);
		euclidFreqs.put("c", expected.get("c") / docSize);
		euclidFreqs.put("d", expected.get("d") / docSize);

		for (String word : actual)
			assertEquals(euclidFreqs.get(word), actual.getEuclideanTermFrequency(word));
	}
	
	@Test
	public void testBiTriCounts() {
		List<String> input = 
			Arrays.asList("a b", "b c", "a b", "a b c", "a c b", "a b", "a c b");
		Map<String, Integer> expected = new HashMap<String, Integer>();
		expected.put("a b", 3);
		expected.put("b c", 1);
		expected.put("a b c",1);
		expected.put("a c b", 2);

		WordCounts actual = new WordCounts(input);
		
		assertEquals(expected, actual.getCounts());
		assertEquals("a b", actual.getMaxNWord(2));
		assertEquals("a c b", actual.getMaxNWord(3));

		double alpha = 0.5;
		Map<String, Double> maxFreqs = new HashMap<String, Double>();
		maxFreqs.put("a b", alpha + (1 - alpha) * 3 / 3);
		maxFreqs.put("b c", alpha + (1 - alpha) * 1 / 3);
		maxFreqs.put("a b c", alpha + (1 - alpha) * 1 / 2);
		maxFreqs.put("a c b", alpha + (1 - alpha) * 2 / 2);

		for (String word : actual)
			assertEquals(maxFreqs.get(word),
					actual.getMaximumTermFrequency(word));

		double docSize2 = Math.sqrt(sqr(expected.get("a b"))
				+ sqr(expected.get("b c")));
		
		double docSize3 = Math.sqrt(sqr(expected.get("a b c"))
				+ sqr(expected.get("a c b")));
		
		
		Map<String, Double> euclidFreqs = new HashMap<String, Double>();
		euclidFreqs.put("a b", expected.get("a b") / docSize2);
		euclidFreqs.put("b c", expected.get("b c") / docSize2);
		euclidFreqs.put("a b c", expected.get("a b c") / docSize3);
		euclidFreqs.put("a c b", expected.get("a c b") / docSize3);

		for (String word : actual)
			assertEquals(euclidFreqs.get(word), actual.getEuclideanTermFrequency(word));
	}


	private static int sqr(int n) {
		return n * n;
	}
}
