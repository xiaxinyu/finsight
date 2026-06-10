import java.sql.*;
public class F5 {
    public static void main(String[] a) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true", "root", "123456");
             Statement s = c.createStatement()) {
            for (String q : new String[]{
                    "SELECT COUNT(*) FROM house_rent",
                    "SELECT COUNT(*) FROM transaction t WHERE EXISTS (SELECT 1 FROM house_rent hr WHERE hr.ID = t.id)",
                    "SELECT COUNT(*) FROM transaction WHERE txn_kind='expense' AND card_type_name LIKE '%租%'"
            }) {
                try (ResultSet rs = s.executeQuery(q)) {
                    rs.next();
                    System.out.println(q + " => " + rs.getInt(1));
                } catch (SQLException e) {
                    System.out.println(q + " => ERR: " + e.getMessage());
                }
            }
            try (ResultSet rs = s.executeQuery("SHOW COLUMNS FROM house_rent LIMIT 5")) {
                System.out.println("house_rent cols:");
                while (rs.next()) System.out.println("  " + rs.getString(1));
            } catch (SQLException e) {
                System.out.println("house_rent: " + e.getMessage());
            }
        }
    }
}
