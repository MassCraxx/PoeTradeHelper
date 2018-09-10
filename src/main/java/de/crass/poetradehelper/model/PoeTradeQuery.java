package de.crass.poetradehelper.model;

public class PoeTradeQuery {

    private final String name;
    private final String type;

    private static String REPLACE_KEY = "%REPLACE_K%";
    private static String REPLACE_VALUE = "%REPLACE_V%";

    public PoeTradeQuery(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getQuery() {
        String replace = "";

        if (name != null && !name.isEmpty()) {
            replace += replacePart.replace(REPLACE_KEY, "name").replace(REPLACE_VALUE, name);
        }
        if (type != null && !type.isEmpty()) {
            if (!replace.isEmpty()) {
                replace += ",\n";
            }
            replace += replacePart.replace(REPLACE_KEY, "type").replace(REPLACE_VALUE, type);
        }

        return defaultQuery.replace(REPLACE_KEY, replace);
    }

    public static String replacePart =
            "    \"" + REPLACE_KEY + "\": \"" + REPLACE_VALUE + "\"";

    public static String defaultQuery = "{\n" +
            "  \"query\": {\n" +
            "    \"status\": {\n" +
            "      \"option\": \"online\"\n" +
            "    },\n" +
            REPLACE_KEY +
            "  },\n" +
            "  \"sort\": {\n" +
            "    \"price\": \"asc\"\n" +
            "  }\n" +
            "}";
}
