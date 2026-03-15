import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DebugMetaAccounts {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5433/saas_db";
        String user = "postgres";
        String password = "1234";
        String sql = """
                select access_token
                from connected_channels
                where id = '7778f92d-05f9-4a43-9b0d-5e0a8a3b3c2b'
                """;

        Class.forName("org.postgresql.Driver");
        String accessToken;
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException("No access token found");
            }
            accessToken = rs.getString("access_token");
        }

        String requestUrl = "https://graph.facebook.com/v23.0/me/accounts"
                + "?fields=" + URLEncoder.encode("id,name,instagram_business_account{id,username,name}", StandardCharsets.UTF_8)
                + "&access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("status=" + response.statusCode());
        System.out.println(response.body());
    }
}
