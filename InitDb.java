import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 一次性建库工具（本地开发用，用完即删）：
 * 1. 连接默认库 postgres，创建 second_hand（若不存在）
 * 2. 连接 second_hand，执行 schema.sql（建表/触发器/种子数据）
 * 用法：java -cp <postgresql.jar> InitDb <password> <schema.sql路径>
 */
public class InitDb {

    public static void main(String[] args) throws Exception {
        String pw = args[0];
        String schemaPath = args[1];

        // 1. 创建数据库
        String createSql = "CREATE DATABASE second_hand ENCODING 'UTF8'";
        try (Connection c = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/postgres", "postgres", pw);
             Statement s = c.createStatement()) {
            s.execute(createSql);
            System.out.println("OK: database second_hand created");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                System.out.println("OK: database second_hand already exists");
            } else {
                throw e;
            }
        }

        // 2. 执行 schema.sql
        String schema = new String(Files.readAllBytes(Path.of(schemaPath)), StandardCharsets.UTF_8);
        try (Connection c = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/second_hand", "postgres", pw);
             Statement s = c.createStatement()) {
            s.execute(schema);
            System.out.println("OK: schema.sql applied to second_hand");
        }
    }
}
