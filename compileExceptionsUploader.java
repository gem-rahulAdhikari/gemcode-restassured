import java.io.*;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
public class compileExceptionsUploader {
    static String executionName;

    public static void main(String[] args) throws IOException {
        Properties reportNameReader = new Properties();
        reportNameReader.load(new FileInputStream("./reportName.properties"));
            executionName = reportNameReader.getProperty("reportName");
            String output=readClassFileAsString("./test-output/"+executionName+".txt");
            if (output.contains("on project RestassuredExecution: Compilation failure")) {
                output = output.split("COMPILATION ERROR :")[1].replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");;
                String classContent = readClassFileAsString("./src/main/java/App.java").replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");
                mongoTransfer(classContent,output);
            }
    }

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

    public static void mongoTransfer(String classContent, String output) throws IOException {
        String userId = executionName.split("_")[1];
        String url = userId;
        try {
            URL getUrl = new URL("https://us-east-1.aws.data.mongodb-api.com/app/application-0-awqqz/endpoint/getSeleniumOutput"); // Replace with your actual GET API URL
            HttpURLConnection getConnection = (HttpURLConnection) getUrl.openConnection();
            getConnection.setRequestMethod("GET");

            int getStatusCode = getConnection.getResponseCode();
            if (getStatusCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(getConnection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    String responseData = response.toString();
                    JSONArray dataArray = new JSONArray(responseData); // Assuming the response is a JSON array

                    String currentUrl = url; // Replace with the URL you want to compare
                    System.out.println(currentUrl);

                    boolean foundMatch = false;
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject item = dataArray.getJSONObject(i);
                        String entryUrl = item.getString("url");

                        if (entryUrl.equals(currentUrl)) {
                            foundMatch = true;

                            String apiUrl = "https://us-east-1.aws.data.mongodb-api.com/app/application-0-awqqz/endpoint/updateSeleniumSubmission";
                            URL url1 = new URL(apiUrl);
                            HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
                            connection.setRequestMethod("PUT");
                            connection.setRequestProperty("Content-Type", "application/json");
                            connection.setDoOutput(true);
                            String putData = "{\n" +
                                    "    \"filter\": {\n" +
                                    "        \"url\": \"" + url + "\"\n" +
                                    "    },\n" +
                                    "    \"SubmittedCode\":\"" + classContent + "\",\n" +
                                    "    \"Output\":\"" + output + "\"\n" +
                                    "}";

                            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                                outputStream.writeBytes(putData);
                                outputStream.flush();
                            }
                            int statusCode = connection.getResponseCode();
                            String statusMessage = connection.getResponseMessage();
                            System.out.println("Status Code: " + statusCode);
                            System.out.println("Status Message: " + statusMessage);
                            break; // No need to continue the loop once a match is found
                        }
                    }

                    if (!foundMatch) {
                        String apiUrl = "https://us-east-1.aws.data.mongodb-api.com/app/application-0-awqqz/endpoint/addSeleniumResult";
                        URL url2 = new URL(apiUrl);
                        HttpURLConnection connection = (HttpURLConnection) url2.openConnection();
                        connection.setRequestMethod("PUT");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);
                        String putData1 = "{\n" +
                                "    \"filter\": {\n" +
                                "        \"url\": \"" + url + "\"\n" +
                                "    },\n" +
                                "    \"SubmittedCode\":\"" + classContent + "\",\n" +
                                "    \"output\":\"" + output + "\"\n" +
                                "}";


                        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                            outputStream.writeBytes(putData1);
                            outputStream.flush();
                        }
                        int statusCode = connection.getResponseCode();
                        String statusMessage = connection.getResponseMessage();
                        System.out.println("Status Code: " + statusCode);
                        System.out.println("Status Message: " + statusMessage);
                    }
                }
            } else {
                System.out.println("GET Request failed with status code " + getStatusCode);
            }

            getConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
