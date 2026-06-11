import java.sql.*;
public class Counts {
  public static void main(String[] a) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true","root","123456");
         Statement s = c.createStatement()) {
      for (String t : new String[]{"cls_category","consume_category","cls_rule","consume_rule","accumulation","_deprecated_medical","medical","endowment","_deprecated_endowment"}) {
        try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM `" + t + "`")) { rs.next(); System.out.println(t+"="+rs.getLong(1)); }
        catch (SQLException e) { System.out.println(t+"=MISSING"); }
      }
    }
  }
}
