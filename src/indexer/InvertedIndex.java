package indexer;

import indexer.dao.DocumentFeatures;
import indexer.dao.InvertedIndexRow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.google.gson.Gson;

import db.wrappers.S3Wrapper;

public class InvertedIndex {
	private static Logger logger = Logger.getLogger(InvertedIndex.class);

	public static final String CREDENTIALS_PROFILE = "default";
	public static final String TABLE_NAME = "InvertedIndex";
	public static final String S3_CRAWL_SNAPSHOT = "cis455-url-content-snapshot5";

	private DynamoDBMapper db;
	private int corpusSize;

	public InvertedIndex() {
		this.db = connectDB();
		S3Wrapper s3 = S3Wrapper.getInstance();
		this.corpusSize = s3.getNumberOfItemsInBucket(S3_CRAWL_SNAPSHOT);
	}

	public static DynamoDBMapper connectDB() {
		AWSCredentials credentials = new ProfileCredentialsProvider(
				CREDENTIALS_PROFILE).getCredentials();
		AmazonDynamoDBClient dbClient = new AmazonDynamoDBClient(credentials);
		dbClient.setRegion(Region.getRegion(Regions.US_EAST_1));

		return new DynamoDBMapper(dbClient);
	}

	public List<InvertedIndexRow> getDocumentLocations(String word) {
		InvertedIndexRow item = new InvertedIndexRow();
		item.setWord(word);

		DynamoDBQueryExpression<InvertedIndexRow> query = new DynamoDBQueryExpression<InvertedIndexRow>()
				.withHashKeyValues(item);
		return db.query(InvertedIndexRow.class, query);
	}

	public static void importData(String fromFile, int batchSize)
			throws IOException {
		DynamoDBMapper db = connectDB();
		BufferedReader br = new BufferedReader(new FileReader(
				new File(fromFile)));
		String line = null;
		List<InvertedIndexRow> rows = new ArrayList<InvertedIndexRow>(batchSize);

		Gson gson = new Gson();
		while ((line = br.readLine()) != null) {
			InvertedIndexRow row = gson.fromJson(line, InvertedIndexRow.class);
			rows.add(row);
			if (rows.size() >= batchSize) {
				db.batchSave(rows);
				logger.info(String
						.format("imported %d records into DynamoDB's 'inverted-index' table.",
								rows.size()));

				rows.clear();
			}
		}
		db.batchSave(rows);
		br.close();
	}

	public PriorityQueue<DocumentScore> rankDocuments(List<String> query) {
		WordCounts queryCounts = new WordCounts(query);
		Map<String, DocumentScore> documentRanks = new HashMap<String, DocumentScore>();
		for (String word : query) {
			// TODO: optimize based on different table layout, multi-thread
			// requests, etc.
			List<InvertedIndexRow> rows = getDocumentLocations(word);
			List<DocumentFeatures> docs = new ArrayList<DocumentFeatures>();
			for (InvertedIndexRow row : rows)
				docs.addAll(row.getFeatures());

			for (DocumentFeatures features : docs) {
				DocumentScore rankedDoc = documentRanks.get(features.getUrl());
				if (rankedDoc == null) {
					rankedDoc = new DocumentScore(word, features);
					documentRanks.put(features.getUrl(), rankedDoc);
				} else {
					rankedDoc.addFeatures(word, features);
				}
				double queryWeight = queryCounts.getTFIDF(word, corpusSize,
						docs.size());
				double docWeight = features.getEuclideanTermFrequency(); // TODO:
																			// try
																			// other
																			// weighting
																			// functions!
				rankedDoc
						.setRank(rankedDoc.getRank() + queryWeight * docWeight);
			}
			logger.info(String.format(
					"=> got %d documents for query word '%s'.", rows.size(),
					word));
		}
		return new PriorityQueue<DocumentScore>(documentRanks.values());
	}

	public static void main(String[] args) {
		try {
			if (args[0].equals("import")) {
				int batchSize = Integer.parseInt(args[2]);
				System.out.println("importing with batchSize " + batchSize
						+ "...");
				importData(args[1], Integer.parseInt(args[2]));
			} else if (args[0].equals("query")) {
				InvertedIndex idx = new InvertedIndex();

				List<String> query = Arrays.asList(Arrays.copyOfRange(args, 1,
						args.length));
				System.out.println("querying for words " + query + "...");

				PriorityQueue<DocumentScore> newResults = idx
						.rankDocuments(query);

				Iterator<DocumentScore> iter = newResults.iterator();
				for (int i = 0; i < 10 && iter.hasNext(); ++i) {
					DocumentScore doc = iter.next();
					System.out.println(doc.toString());
				}

				// System.out.println("============");
				// System.out.println("old results:");
				// System.out.println("============");
				// PriorityQueue<DocumentVector> oldResults =
				// idx.lookupDocuments(query);
				// Iterator<DocumentVector> olditer = oldResults.iterator();
				// for(int i = 0; i < 10 && olditer.hasNext(); ++i) {
				// DocumentVector doc = olditer.next();
				// System.out.println(doc.toString());
				// }
			} else {
				System.out
						.println("usage: InvertedIndex import <fromdir> <batchSize>");
				System.out
						.println("       InvertedIndex query <word1> <word2> ... <wordN>");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}