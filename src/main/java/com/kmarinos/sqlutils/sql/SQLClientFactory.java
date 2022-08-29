package com.kmarinos.sqlutils.sql;

import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLClientFactory {

	public static SQLClient connectTo(String jdbc, String username, String password) {
		return new SimpleSQLClient(deriveDriverClassFromConnectionString(jdbc), jdbc, username, password);
	}
	private static String deriveDriverClassFromConnectionString(String connectionString) {
		if(connectionString==null) {
			return "";
		}
		if(connectionString.contains("jdbc:")) {
			String driverIdentifier=connectionString.split(":", 2)[1];
			if(driverIdentifier.startsWith("oracle")) {
				return "oracle.jdbc.driver.OracleDriver";
			}else if(driverIdentifier.startsWith("as400")) {
				return "com.ibm.as400.access.AS400JDBCDriver";
			}else if(driverIdentifier.startsWith("oracle")) {
				return "oracle.jdbc.driver.OracleDriver";
			}else if(driverIdentifier.startsWith("postgresql")){
				return "org.postgresql.Driver";
			}
			else {
				return "";
			}
		}
		return "";
	}

	private static class SimpleSQLClient extends SQLClient {
		String driverClass;
		String jdbc;
		String username;
		String password;
		String name;
		String environment;

		public SimpleSQLClient(String name, String environment,String driverClass, String jdbc, String username, String password) {
			this.name=name;
			this.environment=environment;
			this.driverClass=driverClass;
			this.jdbc=jdbc;
			this.username=username;
			this.password=password;
		}
		public SimpleSQLClient(String driverClass, String jdbc, String username, String password) {
			this("","",driverClass,jdbc,username,password);
		}

		@Override
		void initConnection() {
			try {
				if(driverClass!=null&&!driverClass.isBlank()) {
					Class.forName(driverClass);					
				}
				conn = DriverManager.getConnection(jdbc, username,password);

				System.out.println("Connection established:" + jdbc);
			} catch (ClassNotFoundException e) {
				System.err.println("No database Driver!");
				return;
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

	}
}
