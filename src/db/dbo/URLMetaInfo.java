package db.dbo;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import db.wrappers.DynamoDBWrapper;

@DynamoDBTable(tableName = "URLMetaInfo")
public class URLMetaInfo {
	private Logger logger = Logger.getLogger(getClass());

	private String url;
	private List<String> outgoingURLs;
	private String type;
	private Integer size;
	private Date lastCrawledOn;
	private String id;

	public String formatHexByteArray(byte[] byteArray) {
		Formatter formatter = new Formatter();
		for (byte b : byteArray) {
			formatter.format("%02x", b);
		}
		String result = formatter.toString().toUpperCase();
		formatter.close();
		return result;
	}

	private String hashUrl(String url) {
		MessageDigest cript = null;
		byte[] encoding = null;
		try {
			cript = MessageDigest.getInstance("SHA-1");
			cript.reset();
			cript.update(url.getBytes("utf8"));
			encoding = cript.digest();
			logger.info("Encoding:" + formatHexByteArray(encoding));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return formatHexByteArray(encoding);
	}

	public URLMetaInfo(String url) {
		this.url = url;
		id = hashUrl(url);
	}

	/**
	 * Do not delete. DynamoDBMapper needs this
	 */
	public URLMetaInfo() {
//		id = UUID.randomUUID().toString();
	}

	@DynamoDBHashKey(attributeName = "url")
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@DynamoDBAttribute(attributeName = "outgoingUrls")
	public List<String> getOutgoingURLs() {
		return outgoingURLs;
	}

	public void setOutgoingURLs(List<String> outgoingURLs) {
		this.outgoingURLs = outgoingURLs;
	}

	@DynamoDBAttribute(attributeName = "type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@DynamoDBAttribute(attributeName = "size")
	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	@DynamoDBAttribute(attributeName = "lastCrawledOn")
	public Date getLastCrawledOn() {
		return lastCrawledOn;
	}

	public void setLastCrawledOn(Date lastCrawledOn) {
		this.lastCrawledOn = lastCrawledOn;
	}

	@DynamoDBAttribute(attributeName = "id")
	@DynamoDBAutoGeneratedKey
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public static List<Object> convertToQuery(List<String> itemIds) {
		List<Object> result = new ArrayList<Object>();
		for (String itemId : itemIds) {
			URLMetaInfo info = new URLMetaInfo(itemId);
			result.add(info);
		}
		return result;
	}

	public static List<String> convertLinksToIds(List<String> links,
			DynamoDBWrapper ddb) {
		List<String> result = new ArrayList<String>();
		Set<String> uSet = new HashSet<String>(links);
		List<String> uLinks = new ArrayList<String>(uSet);
		List<Object> query = URLMetaInfo.convertToQuery(uLinks);
		Map<String, List<Object>> lookupResult = ddb.getBatchItem(query);
		if (lookupResult.get("URLMetaInfo") != null) {
			for (Object infoObject : lookupResult.get("URLMetaInfo")) {
				URLMetaInfo info = (URLMetaInfo) infoObject;
				result.add(info.getId());
			}
		}
		return result;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("url: " + url + " id" + id + " type:" + type + " size:"
				+ size + " lastCrawledOn:" + lastCrawledOn + " outgoing links:"
				+ outgoingURLs);
		return sb.toString();
	}

}
