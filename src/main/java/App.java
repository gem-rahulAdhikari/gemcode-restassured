import io.restassured.RestAssured;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

public class App {
    static String reportName="Report_1709011503_0";
    public Map<String, String> demo() {
        String baseURI = "https://betaapi.gemecosystem.com/gemEcosystemDashboard/actuator/health";
        RestAssured.baseURI = baseURI;
        Response response = RestAssured.given().get().then().extract().response();
        response.prettyPrint();

        // return [base_url, method, params, headers, request body, response_body, status code]
        Map<String, String> x = new HashMap<>();
        x.put("baseURL", baseURI);
        x.put("method", "get");
        x.put("parameters", null);
        x.put("headers", null);
        x.put("payload", null);
        x.put("response", response.asString());
        x.put("status-code", String.valueOf(response.statusCode()));
        return x;
    }
}