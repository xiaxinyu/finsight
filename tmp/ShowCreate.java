import java.sql.*;
import java.util.*;
public class ShowCreate {
  public static void main(String[] a) throws Exception {
    String[] tables = {"transaction","cls_category","cls_rule","cls_rule_tag","imp_staging_entry","statement","fin_bank_account","fs_user","fs_role","fs_user_role","ben_contribution"};
    try (Connection c = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/finsight?useSSL=false&allowPublicKeyRetrieval=true","root","123456");
         Statement s = c.createStatement()) {
      for (String t : tables) {
        try (ResultSet rs = s.executeQuery("SHOW CREATE TABLE `" + t + "`")) {
          if (rs.next()) System.out.println(rs.getString(2) + ";\n");
        } catch (SQLException e) { System.out.println("-- missing " + t); }
      }
    }
  }
}
