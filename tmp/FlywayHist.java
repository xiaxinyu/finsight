import java.sql.*;
public class FlywayHist {
  public static void main(String[] a) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true","root","123456");
         Statement s = c.createStatement();
         ResultSet rs = s.executeQuery("SELECT version, description FROM flyway_schema_history ORDER BY installed_rank")) {
      while (rs.next()) System.out.println(rs.getString(1) + " | " + rs.getString(2));
    }
  }
}
