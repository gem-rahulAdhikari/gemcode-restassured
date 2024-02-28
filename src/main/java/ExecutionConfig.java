import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;


public class ExecutionConfig extends App {
    public static String readClassFileAsString(String filePath) throws IOException {
        //Reading user-updated code
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        return content.toString();
    }

    @Test
    public void main() throws IOException {
        Map<String, String> userResponse = demo();
        String output = "";
        for (Map.Entry<String, String> entry : userResponse.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            output = output + key + " : " + value + System.lineSeparator();
        }
        reportGenerator(output);
    }

    public void reportGenerator(String output) throws IOException {
        //Uploading report to gcloud bucket storage
        System.out.println("Execution complete, report manipulation started");
        Properties reportNameReader = new Properties();
        reportNameReader.load(new FileInputStream("./reportName.properties"));
        String executionName = reportNameReader.getProperty("reportName");
        String uploadStatus = mongoTransfer(executionName, output);
        if (uploadStatus.equalsIgnoreCase("execution data added successfully"))
            System.out.println("Execution complete");
        else if (uploadStatus.equalsIgnoreCase("failed to add execution") || uploadStatus.equalsIgnoreCase("Object not created"))
            System.out.println("Execution complete, failed to upload report");
        else
            System.out.println("User not found");
    }

    public String mongoTransfer(String executionName, String output) throws IOException {
        //upload post data
        String userId = executionName.split("_")[1];
        String classContent = readClassFileAsString("./src/main/java/App.java").replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        output = output.replace("\"", "\\\"").replace("\r", "\\r");
        //check if previous executions are present
        RestAssured.baseURI = "https://us-east-1.aws.data.mongodb-api.com/app/application-0-awqqz/endpoint/getSeleniumOutput";
        Response response = RestAssured.given().queryParam("url", userId).get().then().extract().response();
        if ((response.getBody().jsonPath().get("url")).toString().equalsIgnoreCase("[]"))
            return "User not found";
//        else if (response.getBody().jsonPath().get("Submissions").toString().equalsIgnoreCase("[[]]")) {
//            return "Object not created";
       // }
        else {
            //  System.out.println("upload data put");
            RestAssured.baseURI = "https://us-east-1.aws.data.mongodb-api.com/app/application-0-awqqz/endpoint/updateSeleniumSubmission";
            //    String payload = "{\"filter\": {\"url\":\"\"" + userId + "},\"SubmittedCode\":\"\"" + classContent + ",\"Output\":\"\"" + output + "}";
            String payload = "{\n" +
                    "    \"filter\": {\n" +
                    "        \"url\": \"" + userId + "\"\n" +
                    "    },\n" +
                    "    \"SubmittedCode\":\"" + classContent + "\",\n" +
                    "    \"Output\":\"" + output + "\"\n" +
                    "}";
            response = RestAssured.given().contentType("application/json").body(payload).put().then().extract().response();
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300)
                return "execution data added successfully";
            else
                return "failed to add execution";
        }
    }
}
