/** */
package org.sunbird.notification.fcm.providerImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sunbird.notification.fcm.provider.IFCMNotificationService;
import org.sunbird.notification.utils.FCMResponse;
import org.sunbird.notification.utils.NotificationConstant;

/**
 * This notification service will make http call to send device notification.
 *
 * @author manzarul
 */
public class FCMHttpNotificationServiceImpl implements IFCMNotificationService {
  private static Logger logger = LogManager.getLogger("FCMHttpNotificationServiceImpl");

  /** FCM_URL URL of FCM server */
  public static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";
  /** FCM_ACCOUNT_KEY FCM server key. */
  private static String FCM_ACCOUNT_KEY =
      System.getenv(NotificationConstant.SUNBIRD_FCM_ACCOUNT_KEY);

  private static Map<String, String> headerMap = new HashMap<>();
  private static final String TOPIC_SUFFIX = "/topics/";
  static ObjectMapper mapper = new ObjectMapper();

  static {
    headerMap.put(NotificationConstant.AUTHORIZATION, FCM_ACCOUNT_KEY);
    headerMap.put("Content-Type", "application/json");
  }

  @Override
  public FCMResponse sendSingleDeviceNotification(
      String deviceId, Map<String, String> data, boolean isDryRun) {
    List<String> deviceIds = new ArrayList<String>();
    deviceIds.add(deviceId);
    return sendDeviceNotification(deviceIds, data, FCM_URL, isDryRun);
  }

  @Override
  public FCMResponse sendMultiDeviceNotification(
      List<String> deviceIds, Map<String, String> data, boolean isDryRun) {
    return sendDeviceNotification(deviceIds, data, FCM_URL, isDryRun);
  }

  @Override
  public FCMResponse sendTopicNotification(
      String topic, Map<String, String> data, boolean isDryRun) {
    return sendTopicNotification(topic, data, FCM_URL, isDryRun);
  }

  public static void setAccountKey(String key) {
    FCM_ACCOUNT_KEY = key;
    headerMap.put(NotificationConstant.AUTHORIZATION, FCM_ACCOUNT_KEY);
  }

  /**
   * This method will send notification to FCM.
   *
   * @param topic String
   * @param data Map<String, Object>
   * @param url String
   * @return String as Json.{"message_id": 7253391319867149192}
   */
  private static FCMResponse sendTopicNotification(
      String topic, Map<String, String> data, String url, boolean isDryRun) {
    if (StringUtils.isBlank(FCM_ACCOUNT_KEY) || StringUtils.isBlank(url)) {
      logger.info("FCM account key or URL is not provided===" + FCM_URL);
      return null;
    }
    FCMResponse response = null;
    try {
      JSONObject object1 = new JSONObject(data.get(NotificationConstant.RAW_DATA));
      JSONObject object = new JSONObject();
      object.put(NotificationConstant.DATA, object1);
      object.put(NotificationConstant.DRY_RUN, isDryRun);
      object.put(NotificationConstant.TO, TOPIC_SUFFIX + topic);
      Future<HttpResponse<JsonNode>> httpResponse =
          Unirest.post(FCM_URL)
              .headers(headerMap)
              .body(object.toString())
              .asJsonAsync(
                  new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> httpResponse) {
                      logger.info("on complete method called");
                    }

                    @Override
                    public void failed(UnirestException e) {
                      logger.info("on failed method called");
                      e.printStackTrace();
                    }

                    @Override
                    public void cancelled() {
                      logger.info("on cancelled method called");
                    }
                  });
      ;
      response = mapper.readValue(httpResponse.get().getBody().toString(), FCMResponse.class);
      logger.info("FCM Notification response== for topic " + topic + response);
      // response = mapper.readValue(responsebody, FCMResponse.class);
    } catch (Exception e) {
      logger.info(e.getMessage());
    }
    return response;
  }

  /**
   * This method will send notification to FCM.
   *
   * @param deviceIds list of string
   * @param data Map<String, Object>
   * @param url String
   * @return String as Json.{"message_id": 7253391319867149192}
   */
  private static FCMResponse sendDeviceNotification(
      List<String> deviceIds, Map<String, String> data, String url, boolean isDryRun) {
    if (StringUtils.isBlank(FCM_ACCOUNT_KEY) || StringUtils.isBlank(url)) {
      logger.info("FCM account key or URL is not provided===" + FCM_URL);
      return null;
    }
    FCMResponse fcmResponse = null;
    try {
      JSONObject object1 = new JSONObject(data.get(NotificationConstant.RAW_DATA));
      JSONObject object = new JSONObject();
      object.put(NotificationConstant.DATA, object1);
      object.put(NotificationConstant.DRY_RUN, isDryRun);
      object.put(NotificationConstant.REGISTRATION_IDS, deviceIds);
      Future<HttpResponse<JsonNode>> httpResponse =
          Unirest.post(FCM_URL)
              .headers(headerMap)
              .body(object)
              .asJsonAsync(
                  new Callback<JsonNode>() {
                    @Override
                    public void completed(HttpResponse<JsonNode> httpResponse) {
                      logger.info(
                          "FCM Notification response== for device ids "
                              + deviceIds
                              + " "
                              + httpResponse);
                    }

                    @Override
                    public void failed(UnirestException e) {
                      e.printStackTrace();
                    }

                    @Override
                    public void cancelled() {}
                  });
      fcmResponse = mapper.readValue(httpResponse.get().getBody().toString(), FCMResponse.class);
    } catch (Exception e) {
      logger.info(e.getMessage());
    }
    return fcmResponse;
  }

  @Override
  public FCMResponse sendMultiDeviceNotificationAsync(
      List<String> deviceIds, Map<String, String> data, boolean isDryRun) {
    return null;
  }

  @Override
  public FCMResponse sendTopicNotificationAsync(
      String topic, Map<String, String> data, boolean isDryRun) {
    if (StringUtils.isBlank(FCM_ACCOUNT_KEY) || StringUtils.isBlank(FCM_URL)) {
      logger.info("FCM account key or URL is not provided===" + FCM_URL);
      return null;
    }
    FCMResponse response = null;
    try {
      JSONObject object1 = new JSONObject(data.get(NotificationConstant.RAW_DATA));
      JSONObject object = new JSONObject();
      object.put(NotificationConstant.DATA, object1);
      object.put(NotificationConstant.DRY_RUN, isDryRun);
      object.put(NotificationConstant.TO, TOPIC_SUFFIX + topic);
      Unirest.post(FCM_URL)
          .headers(headerMap)
          .body(object.toString())
          .asJsonAsync(
              new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> httpResponse) {
                  logger.info("on complete method called" + httpResponse.getBody().toString());
                }

                @Override
                public void failed(UnirestException e) {
                  logger.info("on failed method called" + e);
                }

                @Override
                public void cancelled() {
                  logger.info("on cancelled method called");
                }
              });
      response = new FCMResponse();
      logger.info("FCM Notification response== for topic " + topic + response);
    } catch (Exception e) {
      logger.info(e.getMessage());
    }
    return response;
  }
}
