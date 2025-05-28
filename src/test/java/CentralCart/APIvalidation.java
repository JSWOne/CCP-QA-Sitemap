package CentralCart;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;

    public class APIvalidation
    {

        public static void main(String[] args) throws Exception {
            // 🔐 Make the GET call with API Key
            Response response = RestAssured
                    .given()
                    .header("x-api-key", "zk9656eke06c6gp4ombsc5thknmqz3hho33xwi59g8fq12vx8oatr2fmxl3qmyvz10keukce1a1h")  // ⬅️ Replace this with your actual key
                    .get("https://qa.msme.jswone.in/central-cart/api/internal/v1/cart/add"); // ⬅️ Replace with your actual endpoint

            response.then().statusCode(200); // Make sure the API returned 200 OK

            // ✅ Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody().asString());

            // ✅ Start validation
            validateJsonNode("", root);
        }

        public static void validateJsonNode(String path, JsonNode node) {
            if (node.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> field = it.next();
                    String currentPath = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
                    validateJsonNode(currentPath, field.getValue());
                }
            } else if (node.isArray()) {
                int index = 0;
                for (JsonNode item : node) {
                    validateJsonNode(path + "[" + index + "]", item);
                    index++;
                }
            } else {
                // Leaf node: validate it
                if (node.isNull()) {
                    System.out.println("❌ NULL at: " + path);
                } else if (node.isTextual()) {
                    String val = node.asText().trim();
                    if (val.isEmpty()) {
                        System.out.println("❌ EMPTY string at: " + path);
                    } else if (val.equalsIgnoreCase("null")) {
                        System.out.println("❌ 'null' as string at: " + path + " -> \"" + val + "\"");
                    }
                }
            }
        }
    }
