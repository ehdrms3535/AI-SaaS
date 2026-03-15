import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DebugConnectedChannel {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5433/saas_db";
        String user = "postgres";
        String password = "1234";
        String sql = """
                select id, organization_id, provider, status, external_account_id, account_name, username,
                       substring(access_token, 1, 24) as access_token_prefix,
                       token_expires_at
                from connected_channels
                order by created_at desc
                """;

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                System.out.println("id=" + rs.getString("id"));
                System.out.println("organization_id=" + rs.getString("organization_id"));
                System.out.println("provider=" + rs.getString("provider"));
                System.out.println("status=" + rs.getString("status"));
                System.out.println("external_account_id=" + rs.getString("external_account_id"));
                System.out.println("account_name=" + rs.getString("account_name"));
                System.out.println("username=" + rs.getString("username"));
                System.out.println("access_token_prefix=" + rs.getString("access_token_prefix"));
                System.out.println("token_expires_at=" + rs.getString("token_expires_at"));
                System.out.println("---");
            }
        }
    }
}
