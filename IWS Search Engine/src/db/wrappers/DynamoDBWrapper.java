package db.wrappers;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;

public class DynamoDBWrapper {
	private final Logger logger = Logger.getLogger(DynamoDBWrapper.class);
	private static DynamoDBWrapper wrapper;
	private AmazonDynamoDBClient client;
	private String endPoint;
	private DynamoDB dynamoDB;
	private DynamoDBMapper mapper;

	public DynamoDBMapper getMapper() {
		return mapper;
	}

	public String getEndPoint() {
		return endPoint;
	}

	private DynamoDBWrapper(String endPoint) {
		client = new AmazonDynamoDBClient(new ProfileCredentialsProvider(
				"shreejit"));
		this.endPoint = endPoint;
		client.setEndpoint(this.endPoint);
		dynamoDB = new DynamoDB(client);
		mapper = new DynamoDBMapper(client);
	}

	public DescribeTableResult describeTable(String tableName) {
		DescribeTableResult result = null;
		try {
			result = client.describeTable(tableName);
		} catch (ResourceNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static DynamoDBWrapper getInstance(String endPoint) {
		if (wrapper == null || !wrapper.getEndPoint().equals(endPoint)) {
			Logger.getLogger(DynamoDBWrapper.class).warn(
					"Setting endpoint to " + endPoint);
			wrapper = new DynamoDBWrapper(endPoint);
		}
		return wrapper;
	}

	/**
	 * Retrieves the object from dynamodb as identified by the itemId. This
	 * assumes that you have annotated the class that you want to retrieve with
	 * this get method with the appropriate dynamodb annotations. The class also
	 * SHOULD have a default constructor, otherwise it cannot be instantiated by
	 * dynamodb mapper
	 * 
	 * @param itemId
	 *            is the record identifier
	 * @param clazz
	 *            the dynamodb annotated record class
	 * @return
	 */
	public Object getItem(String itemId, Class clazz) {
		Object obj = mapper.load(clazz, itemId);
		return obj;
	}

	/**
	 * 
	 * @param tableName
	 * @param toSave
	 *            this assumes that you have annotated the class object with the
	 *            appropriate annotations that allows the mapper to make object
	 *            bindings
	 */
	public void putItem(Object toSave) {
		mapper.save(toSave);
	}

	public void deleteTable(String tableName) {
		Table table = dynamoDB.getTable(tableName);
		try {
			logger.warn("Issuing DeleteTable request for " + tableName);
			table.delete();
			logger.warn("Waiting for " + tableName
					+ " to be deleted...this may take a while...");
			table.waitForDelete();

		} catch (Exception e) {
			System.err.println("DeleteTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}
	}

	public void createTable(String tableName, long readCapacityUnits,
			long writeCapacityUnits, String partitionKeyName,
			String partitionKeyType) {

		if(describeTable(tableName) == null) {
		createTable(tableName, readCapacityUnits, writeCapacityUnits,
				partitionKeyName, partitionKeyType, null, null);
		} else {
			logger.error("DynamoDB table " + tableName + " already exists! Not creating it");
		}
	}

	public void createTable(String tableName, long readCapacityUnits,
			long writeCapacityUnits, String partitionKeyName,
			String partitionKeyType, String sortKeyName, String sortKeyType) {

		try {

			ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
			keySchema.add(new KeySchemaElement().withAttributeName(
					partitionKeyName).withKeyType(KeyType.HASH)); // Partition
																	// key

			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition()
					.withAttributeName(partitionKeyName).withAttributeType(
							partitionKeyType));

			if (sortKeyName != null) {
				keySchema.add(new KeySchemaElement().withAttributeName(
						sortKeyName).withKeyType(KeyType.RANGE)); // Sort key
				attributeDefinitions.add(new AttributeDefinition()
						.withAttributeName(sortKeyName).withAttributeType(
								sortKeyType));
			}

			CreateTableRequest request = new CreateTableRequest()
					.withTableName(tableName)
					.withKeySchema(keySchema)
					.withProvisionedThroughput(
							new ProvisionedThroughput().withReadCapacityUnits(
									readCapacityUnits).withWriteCapacityUnits(
									writeCapacityUnits));

			request.setAttributeDefinitions(attributeDefinitions);

			logger.warn("Issuing CreateTable request for " + tableName);
			Table table = dynamoDB.createTable(request);
			logger.warn("Waiting for " + tableName
					+ " to be created...this may take a while...");
			table.waitForActive();

		} catch (Exception e) {
			System.err.println("CreateTable request failed for " + tableName);
			System.err.println(e.getMessage());
		}
	}

}