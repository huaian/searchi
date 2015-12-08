package utils.nlp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

public class LanguageDetector {

	private static final Logger logger = Logger
			.getLogger(LanguageDetector.class);
	private static Integer TOKEN_HEURISTIC = 10;
	private static Double TOKEN_HEURISTIC_THRESHHOLD = 70.0;

	public static List<String> mostCommonEnglishWords = new ArrayList<String>() {
		{
			add("the");
			add("be");
			add("to");
			add("of");
			add("and");
			// add("a");
			add("in");
			add("that");
			add("have");
			add("I");
		}
	};

	public static int englishWordCountHeuristic = 4;

	public static boolean hasEnglishHeader(String content) {
		if (content.contains("<meta http-equiv=\"Content-Language\"")) {
			if (content
					.contains("<meta http-equiv=\"Content-Language\" content=\"en\"")) {
				return true;

			} else {
				return false;
			}
		}
		if (content.contains("lang=\"")) {
//			logger.info("Content contains lang");
			if (content.contains("lang=\"en\"")) {
//				logger.info("Content contains lang=\"en\"");
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	public static boolean isEnglish(String content) {

		int englishWordCount = 0;
		Set<String> seenWords = new HashSet<String>();
		for (String englishWord : mostCommonEnglishWords) {
			if (content.toLowerCase().contains(" " + englishWord + " ")
					&& !seenWords.contains(englishWord)) {
				englishWordCount++;
				seenWords.add(englishWord);
				// logger.info("Found " + englishWord +
				// " in content. Total count " + englishWordCount);
				continue;
			}
		}
		return (englishWordCount >= englishWordCountHeuristic) && hasEnglishHeader(content);
	}

}
