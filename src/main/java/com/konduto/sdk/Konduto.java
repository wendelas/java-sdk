package com.konduto.sdk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konduto.sdk.exceptions.KondutoHTTPException;
import com.konduto.sdk.exceptions.KondutoHTTPExceptionFactory;
import com.konduto.sdk.exceptions.KondutoInvalidEntityException;
import com.konduto.sdk.exceptions.KondutoUnexpectedAPIResponseException;
import com.konduto.sdk.models.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Konduto is an HTTP Client for connecting to Konduto's API.
 *
 */

public final class Konduto {
	private static String apiKey;
	private static JsonObject requestBody;
	private static JsonObject responseBody;
	private static URI endpoint = URI.create("https://api.konduto.com/v1");

	/**
	 *
	 * @param endpoint Konduto's API endpoint (default is https://api.konduto.com/v1)
	 */
	public static void setEndpoint(URI endpoint) {
		Konduto.endpoint = endpoint;
	}

	private Konduto() { /* cannot be instantiated */  }

	/**
	 *
	 * @param apiKey sets the merchant secret API key, which is required for Konduto's API authentication.
	 */
	public static void setApiKey(String apiKey) {
		if (apiKey == null || apiKey.length() != 21) {
			throw new IllegalArgumentException("Illegal API Key: " + apiKey);
		}
		Konduto.apiKey = apiKey;
	}


	/**
	 * Helper method to debug requests made to Konduto's API.
	 * @return a String containing API Key, Konduto's API endpoint, request and response bodies.
	 */
	public static String debug() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("API Key: %s\n", Konduto.apiKey));
		sb.append(String.format("Endpoint: %s\n", Konduto.endpoint.toString()));
		if(Konduto.requestBody != null) {
			sb.append(String.format("Request body: %s\n", Konduto.requestBody));
		}
		if(Konduto.responseBody != null) {
			sb.append(String.format("Response body: %s\n", Konduto.responseBody));
		}
		return sb.toString();
	}

	/**
	 * @param orderId the order identifier
	 * @return [GET] order URI (ENDPOINT/orders/orderId)
	 */
	protected static URI kondutoGetOrderUrl(String orderId) {
		return URI.create(endpoint.toString().concat("/orders/" + orderId));
	}

	/**
	 * @return [POST] order URI (ENDPOINT/orders)
	 */
	protected static URI kondutoPostOrderUrl(){
		return URI.create(endpoint.toString().concat("/orders"));
	}

	/**
	 *
	 * @param orderId the order identifier
	 * @return [PUT] order URI (ENDPOINT/orders/orderId)
	 */
	protected static URI kondutoPutOrderUrl(String orderId) {
		return URI.create(endpoint.toString().concat("/orders/" + orderId));
	}

	/**
	 * @param method the HTTP method
	 * @return the response body as a {@link com.google.gson.JsonObject JsonObject}
	 * @throws IOException
	 */
	private static JsonObject extractResponse(HttpMethod method) throws IOException {
		byte[] responseBodyAsByteArray = method.getResponseBody();
		String responseBodyAsString = new String(responseBodyAsByteArray, "UTF-8");
		JsonParser parser = new JsonParser();
		return (JsonObject) parser.parse(responseBodyAsString);
	}

	/**
	 * @param requestBody the HTTP request body
	 * @return a request entity
	 * @throws UnsupportedEncodingException
	 */
	private static StringRequestEntity getRequestEntity(JsonObject requestBody) throws UnsupportedEncodingException {
		return new StringRequestEntity(
				requestBody.toString(),
				"application/json",
				"UTF-8"
		);
	}


	/**
	 *
	 * @param method the HTTP method
	 * @param requestBody the HTTP request body
	 * @return the response coming from Konduto's API
	 * @throws KondutoHTTPException when something goes wrong, i.e a non-200 OK response is answered
	 */
	private static JsonObject sendRequest(HttpMethod method, JsonObject requestBody) throws KondutoHTTPException {

		if(apiKey == null) { throw new NullPointerException("API key cannot be null"); }

		JsonObject responseBody;

		HttpClient httpClient = new HttpClient();

		String base64 = null;
		try {
			base64 = new String(Base64.encodeBase64(apiKey.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			base64 = new String(Base64.encodeBase64(apiKey.getBytes()));
		} finally {
			method.addRequestHeader("Authorization", "Basic " + base64);
		}

		try {
			if(method instanceof PostMethod) {
				Konduto.requestBody = requestBody; // set Konduto's request body for debugging purposes
				((PostMethod) method).setRequestEntity(getRequestEntity(requestBody));
			} else if (method instanceof PutMethod) {
				Konduto.requestBody = requestBody; // set Konduto's request body for debugging purposes
				((PutMethod) method).setRequestEntity(getRequestEntity(requestBody));
			}

			int statusCode = httpClient.executeMethod(method);

			responseBody = extractResponse(method);

			Konduto.responseBody = responseBody; // set Konduto's response body for debugging purposes

			if(statusCode != 200) { throw KondutoHTTPExceptionFactory.buildException(statusCode, responseBody); }

			return responseBody;

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			method.releaseConnection();
		}

		return null;
	}

	/**
	 * Queries an order from Konduto's API.
	 *
	 * @param orderId the order identifier
	 * @return a {@link KondutoOrder} instance
	 * @throws KondutoHTTPException
	 * @throws KondutoUnexpectedAPIResponseException
	 *
	 * @see <a href="http://docs.konduto.com">Konduto API Spec</a>
	 */
	public static KondutoOrder getOrder(String orderId)
			throws KondutoHTTPException, KondutoUnexpectedAPIResponseException {
		GetMethod getMethod = new GetMethod(kondutoGetOrderUrl(orderId).toString());

		JsonObject responseBody;

		responseBody = sendRequest(getMethod, null);

		if(responseBody == null) {
			throw new KondutoUnexpectedAPIResponseException(responseBody);
		}

		if(responseBody.has("order")) responseBody = responseBody.getAsJsonObject("order"); // unwrapping

		KondutoOrder order = (KondutoOrder) KondutoModel.fromJSON(responseBody, KondutoOrder.class);

		if(order.getId() == null) { order.setId(orderId); }

		return order ;
	}

	/**
	 * Sends an order for Konduto and gets it analyzed
	 * (i.e with recommendation, score, device, geolocation and navigation info).
	 *
	 * @param order a {@link KondutoOrder} instance
	 * @throws KondutoInvalidEntityException
	 * @throws KondutoHTTPException
	 * @throws KondutoUnexpectedAPIResponseException
	 *
	 * @see <a href="http://docs.konduto.com">Konduto API Spec</a>
	 */
	public static void analyze(KondutoOrder order)
			throws KondutoInvalidEntityException, KondutoHTTPException, KondutoUnexpectedAPIResponseException {

		PostMethod postMethod = new PostMethod(kondutoPostOrderUrl().toString());

		JsonObject responseBody;

		responseBody = sendRequest(postMethod, order.toJSON());

		if(responseBody == null) {
			throw new KondutoUnexpectedAPIResponseException(responseBody);
		}

		if(responseBody.has("order")) responseBody = responseBody.getAsJsonObject("order"); // unwrapping

		if(responseBody.has("status")){
			order.setStatus(KondutoOrderStatus.valueOf(responseBody.get("status").getAsString().toUpperCase()));
		}

		if(responseBody.has("score")){
			order.setScore(responseBody.get("score").getAsDouble());
		}

		if(responseBody.has("recommendation")){
			order.setRecommendation(KondutoRecommendation.valueOf(responseBody.get("recommendation").getAsString().toUpperCase()));
		}

		if(responseBody.has("geolocation")){
			order.setGeolocation((KondutoGeolocation) KondutoModel.fromJSON(responseBody.getAsJsonObject("geolocation"), KondutoGeolocation.class));
		}

		if(responseBody.has("device")){
			order.setDevice((KondutoDevice) KondutoModel.fromJSON(responseBody.getAsJsonObject("device"), KondutoDevice.class));
		}

		if(responseBody.has("navigation")){
			order.setNavigationInfo((KondutoNavigationInfo) KondutoModel.fromJSON(responseBody.getAsJsonObject("navigation"), KondutoNavigationInfo.class));
		}

	}

	/**
	 * Updates an order status.
	 *
	 * @param order the order to update
	 * @param newStatus the new status (APPROVED, DECLINED or FRAUD)
	 * @param comments some comments (an empty String is accepted, although we recommend setting it to at least a timestamp)
	 * @throws KondutoHTTPException
	 * @throws KondutoUnexpectedAPIResponseException
	 *
	 * @see <a href="http://docs.konduto.com">Konduto API Spec</a>
	 */

	public static void updateOrderStatus(KondutoOrder order, KondutoOrderStatus newStatus, String comments)
			throws KondutoHTTPException, KondutoUnexpectedAPIResponseException {

		List<KondutoOrderStatus> allowedStatuses = Arrays.asList(
				KondutoOrderStatus.APPROVED,
				KondutoOrderStatus.DECLINED,
				KondutoOrderStatus.FRAUD
		);

		if (!allowedStatuses.contains(newStatus)){ throw new IllegalArgumentException("Illegal status: " + newStatus); }
		if (comments == null){ throw new NullPointerException("comments cannot be null"); }

		PutMethod putMethod = new PutMethod(kondutoPutOrderUrl(order.getId()).toString());

		JsonObject requestBody = new JsonObject();

		requestBody.addProperty("status", newStatus.toString().toLowerCase());
		requestBody.addProperty("comments", comments);

		JsonObject responseBody;

		responseBody = sendRequest(putMethod, requestBody);

		if(responseBody.has("order")) responseBody = responseBody.getAsJsonObject("order"); // unwrapping

		if (responseBody == null || !responseBody.has("old_status") || !responseBody.has("new_status")) {
			throw new KondutoUnexpectedAPIResponseException(responseBody);
		}

		order.setStatus(newStatus);



	}
}