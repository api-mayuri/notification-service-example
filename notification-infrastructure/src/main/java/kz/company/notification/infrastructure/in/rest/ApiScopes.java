package kz.company.notification.infrastructure.in.rest;

/** OAuth2 scopes API. В Spring Security им соответствуют полномочия {@code SCOPE_<scope>}. */
public final class ApiScopes {

    public static final String SEND = "notifications:send";
    public static final String SEND_RAW = "notifications:send-raw";
    public static final String READ = "notifications:read";
    public static final String TEMPLATES = "templates:manage";
    public static final String PREFERENCES = "preferences:manage";

    private ApiScopes() {
    }

    public static String authority(String scope) {
        return "SCOPE_" + scope;
    }
}
