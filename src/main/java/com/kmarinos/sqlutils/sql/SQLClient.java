package com.kmarinos.sqlutils.sql;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class SQLClient {

	protected Connection conn;
	
	//Initialize the connection and set it to this.conn
	abstract void initConnection();
		
	protected Connection getValidConnection() {
		try {
			if(conn==null||conn.isClosed()) {
				initConnection();
			}
		} catch (SQLException e) {
			initConnection();
		}
		return conn;
	}
	
	public SQLExecutor select(String sql) {
		return new SQLExecutor(sql,this);
	}
}
