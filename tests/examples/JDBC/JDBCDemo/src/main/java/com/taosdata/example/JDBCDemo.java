package com.taosdata.example;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.sql.*;
import java.util.Calendar;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class JDBCDemo {
    private static String host;
    private static final String dbName = "test";
    private static final String tbName = "weather";
    private Connection connection;

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < args.length; i++) {
            if ("-host".equalsIgnoreCase(args[i]) && i < args.length - 1)
                host = args[++i];
        }
        if (host == null) {
            printHelp();
        }
        JDBCDemo demo = new JDBCDemo();
        demo.init();
        demo.createDatabase();
        demo.useDatabase();
        demo.dropTable();
        demo.createTable();

        demo.insertBatch();

//        demo.insert();
//        demo.select();
//        demo.dropTable();
        demo.close();
    }

    private void insertBatch() throws InterruptedException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -5);
        for (int i = 0; i < 100; i++) {
            insertSingle();
        }
        Thread.sleep(1000);
    }

    private void insertSingle() {
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "开始插入数据");
            String sql = "";
            for (int j = 0; j < 1000000; j++) {
                String id = UUID.randomUUID().toString().replace("-", "");
                Calendar calendar = Calendar.getInstance();
                for (int i = 0; i < 10000; i++) {
                    if (i % 500 == 0 && StringUtils.isNotEmpty(sql)) {
                        sql = "INSERT INTO base_" + id + " USING " + tbName + " TAGS ('" + id + "') VALUES" + sql;
                        exuete(sql);
                        sql = "";
                    } else {
                        calendar.add(Calendar.MINUTE, -new Random().nextInt(10));
                        String format = DateFormatUtils.format(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
                        sql += " ('" + format + "', " + new Random().nextDouble() * 10000 + ")";
                    }
                }
                System.out.println(Thread.currentThread().getName() + ",第" + j + "个参数：" + id + "完成10000条写入");
            }
            System.out.println(Thread.currentThread().getName() + "插入完毕");
        }).start();
    }

    private void init() {
        final String url = "jdbc:TAOS://" + host + ":6030/?user=root&password=taosdata";
        // get connection
        try {
            Class.forName("com.taosdata.jdbc.TSDBDriver");
            Properties properties = new Properties();
            properties.setProperty("charset", "UTF-8");
            properties.setProperty("locale", "en_US.UTF-8");
            properties.setProperty("timezone", "UTC-8");
            System.out.println("get connection starting...");
            connection = DriverManager.getConnection(url, properties);
            if (connection != null)
                System.out.println("[ OK ] Connection established.");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void createDatabase() {
        String sql = "create database if not exists " + dbName;
        exuete(sql);
    }

    private void useDatabase() {
        String sql = "use " + dbName;
        exuete(sql);
    }

    private void dropTable() {
        final String sql = "drop table if exists " + dbName + "." + tbName + "";
        exuete(sql);
    }

    private void createTable() {
        final String sql = "create table if not exists " + dbName + "." + tbName + " (ts timestamp, temperature float, humidity int)";
        exuete(sql);
    }

    private void insert() {
        final String sql = "insert into test.weather (ts, temperature, humidity) values(now, 20.5, 34)";
        exuete(sql);
    }

    private void select() {
        final String sql = "select * from test.weather";
        executeQuery(sql);
    }

    private void close() {
        try {
            if (connection != null) {
                this.connection.close();
                System.out.println("connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /************************************************************************/

    private void executeQuery(String sql) {
        try (Statement statement = connection.createStatement()) {
            long start = System.currentTimeMillis();
            ResultSet resultSet = statement.executeQuery(sql);
            long end = System.currentTimeMillis();
            printSql(sql, true, (end - start));
            printResult(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void printResult(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        while (resultSet.next()) {
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnLabel = metaData.getColumnLabel(i);
                String value = resultSet.getString(i);
                System.out.printf("%s: %s\t", columnLabel, value);
            }
            System.out.println();
        }
    }


    private void printSql(String sql, boolean succeed, long cost) {
        System.out.println("[ " + (succeed ? "OK" : "ERROR!") + " ] time cost: " + cost + " ms, execute statement ====> " + sql);
    }

    private void exuete(String sql) {
        try (Statement statement = connection.createStatement()) {
            long start = System.currentTimeMillis();
            boolean execute = statement.execute(sql);
            long end = System.currentTimeMillis();
            printSql(sql, execute, (end - start));
        } catch (SQLException e) {
            e.printStackTrace();

        }
    }

    private static void printHelp() {
        System.out.println("Usage: java -jar JDBCDemo.jar -host <hostname>");
        System.exit(0);
    }


}
