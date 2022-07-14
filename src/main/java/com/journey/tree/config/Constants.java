/**
 * @author Sacumen(www.sacumen.com)
 * <p>
 * Constants class which defines constants field which will be used
 * throughout the application
 */
package com.journey.tree.config;

public final class Constants {

    private Constants() {
    }

    public static final String API_TOKEN_URL = "https://app.journeyid.io/api/system/token";
    public static final String ENROLLMENTS_CHECK_URL = "https://app.journeyid.io/api/system/customers/lookup";
    public static final String EXECUTION_RETRIEVE = "https://app.journeyid.io/api/system/executions/";
    public static final String CREATE_EXECUTION_URL = "https://app.journeyid.io/api/system/executions";
    public static final String FORGEROCK_GET_TOKEN_URL = "/json/realms/root/authenticate";
    public static final String FORGEROCK_GET_GROUP_MEMBERS_URL = "/json/realms/root/groups/";
    public static final String FORGEROCK_GET_SERVER_INFO_URL = "/json/serverinfo/*";
    public static final String FORGEROCK_GET_SESSION_INFO_URL = "/json/sessions?_action=getSessionInfo";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String API_ACCESS_TOKEN = "api_access_token";
    public static final String ACCOUNT_ID = "account_id";
    public static final String IS_ADMIN = "is_admin";
    public static final String FORGEROCK_SESSION_ID = "forgerock_session_id";
    public static final String CUSTOMER_JOURNEY_ID = "customer_journey_id";
    public static final String METHOD_NAME = "method_name";
    public static final String FACIAL_BIOMETRIC = "facial-biometrics";
    public static final String MOBILE_APP = "mobile-app";
    public static final String ONE_TIME_PASSWORD = "one-time-password";
    public static final String JOURNEY_PHONE_NUMBER = "journey_phone_number";
    public static final String FORGEROCK_PHONE_NUMBER = "forgerock_phone_number";
    public static final String FORGEROCK_EMAIL = "forgerock_email";
    public static final String JOURNEY_EMAIL = "journey_email";
    public static final String DEVICE_ID = "device_id";
    public static final String CUSTOMER_LOOKUP_RESPONSE_CODE = "customer_lookup_response_code";
    public static final String TYPE = "type";
    public static final String PIPELINE_KEY = "pipeline_key";
    public static final String DASHBOARD_ID = "dashboard_id";
    public static final String EXECUTION_COMPLETED = "execution_completed";
    public static final String EXECUTION_FAILED = "execution_failed";
    public static final String EXECUTION_TIMEOUT = "execution_timeout";
    public static final String FORGEROCK_ID = "forgerock_id";
    public static final String TOKEN_ID = "token_id";
    public static final String COOKIE_NAME = "cookie_name";
    public static final String UNIQUE_IDENTIFIER_USERNAME = "username";
    public static final String UNIQUE_IDENTIFIER_EMAIL = "email";
    public static final String UNIQUE_IDENTIFIER_FORGEROCK_ID = "forgerock_id";
    public static final String UNIQUE_ID = "unique_id";
    public static final String COUNTER = "counter";
    public static final String REQUEST_TIMEOUT = "request_timeout";
    public static final String ERROR_MESSAGE = "error_message";
    public static final String RETRIEVE_TIMEOUT = "retrieve_timeout";
    public static final String RETRIEVE_DELAY = "retrieve_delay";
    public static final String FORGEROCK_HOST_URL = "forgerock_host_url";
    public static final String EXECUTION_STATUS = "execution_status";
    public static final String RETRIEVE_API_CONNECTION = "retrieve_api_connection";
}