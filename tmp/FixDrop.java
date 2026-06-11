import java.sql.*;
public class FixDrop {
  public static void main(String[] a) throws Exception {
    try (Connection c = DriverManager.getConnection(
        "jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC","root","123456");
         Statement s = c.createStatement()) {
      String[] names = {"_archive_consume_category","_archive_consume_rule","_archive_consume_rule_tag"};
      for (String n : names) {
        try (ResultSet rs = s.executeQuery("SELECT table_type FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='"+n+"'")) {
          if (rs.next()) System.out.println(n + " -> " + rs.getString(1));
          else System.out.println(n + " -> missing");
        }
        s.execute("DROP VIEW IF EXISTS `" + n + "`");
        s.execute("DROP TABLE IF EXISTS `" + n + "`");
      }
      try (ResultSet rs = s.executeQuery("SELECT table_name, table_type FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name LIKE '_archive%' ORDER BY 1")) {
        while (rs.next()) System.out.println("left: " + rs.getString(1) + " " + rs.getString(2));
      }
    }
  }
}
