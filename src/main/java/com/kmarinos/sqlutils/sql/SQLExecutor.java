package com.kmarinos.sqlutils.sql;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLExecutor {

  String sql;
  List<Object> inputParams = new ArrayList<>();
  long parameterCount = 0;
  final SQLClient client;

  public SQLExecutor(String sql, SQLClient client) {
    this.sql = sql;
    this.client = client;
    this.parameterCount = sql.chars().filter(c -> c == '?').count();
  }

  public SQLExecutor withParams(List<? extends Object> inputParams) {
    if (inputParams == null || inputParams.size() != parameterCount) {
      throw new RuntimeException(
          "The input parameters don't match the parameters defined in the sql statement.");
    }
    inputParams.forEach(this::withParam);
    return this;
  }

  public SQLExecutor withParam(Object inputParam) {
    this.getInputParams().add(inputParam);
    return this;
  }

  public <T> List<T> andGetAs(Class<T> resultClass) {
    return this.andCollect(mapFromReflection(resultClass));
  }

  public void andConsume(Consumer<SelectResult> consumer) {
    this.andCollect(s -> {
      consumer.accept(s);
      return null;
    });
  }
  public <T>void andConsumeAs(Class<T>resultClass,Consumer<T> consumer){
    this.andConsume(s->{
      consumer.accept(mapFromReflection(resultClass).apply(s));
    });
  }

  public List<SelectResult> andGet() {
    return this.andCollect(Function.identity());
  }

  public <T> List<T> andCollect(Function<SelectResult, T> c) {
    List<T> resultList = new ArrayList<T>();
    try {
      PreparedStatement pstmt = client.getValidConnection().prepareStatement(this.sql);
      for (int i = 0; i < inputParams.size(); i++) {
        pstmt.setObject(i + 1, inputParams.get(i));
      }
      ResultSet rs = pstmt.executeQuery();
      ResultSetMetaData meta = rs.getMetaData();
      while (rs.next()) {
        SelectResult ctx = new SelectResult();
        for (int idx = 0; idx < meta.getColumnCount(); idx++) {
          String columnName = meta.getColumnName(idx + 1);
          Class<?> columnType;
          Object value;
          try {
            columnType = Class.forName(JDBC_TYPE_MAP.get(meta.getColumnTypeName(idx + 1).toUpperCase()));
            value = columnType.cast(rs.getObject(idx + 1));
          } catch (ClassNotFoundException e) {
            columnType = String.class;
            value = rs.getString(idx + 1);
          } catch(ClassCastException ex){
            try{
              columnType = Class.forName(JDBC_TYPE_MAP.get(JDBCType.valueOf(meta.getColumnType(idx+1)).name().toUpperCase()));
              value = columnType.cast(rs.getObject(idx+1));
            }catch (ClassNotFoundException e){
              columnType = String.class;
              value = rs.getString(idx + 1);
            }
          }
          ctx.put(columnName, columnType, value);

        }
        resultList.add(c.apply(ctx));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return resultList;
  }

  public String getSql() {
    return sql;
  }

  public List<Object> getInputParams() {
    return inputParams;
  }

  public void setInputParams(List<Object> inputParams) {
    this.inputParams = inputParams;
  }

  private <T> Function<SelectResult, T> mapFromReflection(Class<T> forClass) {
    return map -> {
      T obj = null;
      try {
        obj = forClass.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
               NoSuchMethodException e) {
        System.err.println(
            "Cannot create Object for class " + forClass + " from default constructor");
      }
      for (Field declaredField : forClass.getDeclaredFields()) {
        var value = map.get(declaredField.getName());
        if(value==null){
          value = map.get(camelToSnake(declaredField.getName()));
        }
        if (value != null) {
          declaredField.setAccessible(true);
          try {
            declaredField.set(obj, value);
          } catch (IllegalAccessException e) {
            System.err.println("Cannot set value for field " + declaredField.getName());
          }
        }
      }
      return obj;
    };
  }
  private String camelToSnake(String str)
  {

    // Empty String
    String result = "";

    // Append first character(in lower case)
    // to result string
    char c = str.charAt(0);
    result = result + Character.toLowerCase(c);

    // Traverse the string from
    // ist index to last index
    for (int i = 1; i < str.length(); i++) {

      char ch = str.charAt(i);

      // Check if the character is upper case
      // then append '_' and such character
      // (in lower case) to result string
      if (Character.isUpperCase(ch)) {
        result = result + '_';
        result
            = result
            + Character.toLowerCase(ch);
      }

      // If the character is lower case then
      // add such character into result string
      else {
        result = result + ch;
      }
    }

    // return the result
    return result;
  }
  private String snakeToCamel(String str)
  {
    // Capitalize first letter of string
    str = str.substring(0, 1).toUpperCase()
        + str.substring(1);

    // Run a loop till string
    // string contains underscore
    while (str.contains("_")) {

      // Replace the first occurrence
      // of letter that present after
      // the underscore, to capitalize
      // form of next letter of underscore
      str = str
          .replaceFirst(
              "_[a-z]",
              String.valueOf(
                  Character.toUpperCase(
                      str.charAt(
                          str.indexOf("_") + 1))));
    }
    // Return string
    return str.substring(0, 1).toLowerCase(Locale.ROOT) + str.substring(1);
  }

  /*
   * Created from https://download.oracle.com/otn-pub/jcp/jdbc-4_2-mrel2-spec/jdbc4.2-fr-spec.pdf?AuthParam=1635157576_a8d8e5d1dacd71494befa6cf4f7f9473
   * Appendix B3
   *
   */
  private static final Map<String, String> JDBC_TYPE_MAP = Stream.of(new String[][]{
      {"CHAR","java.lang.String"},
      {"VARCHAR","java.lang.String"},
      {"VARCHAR2","java.lang.String"},
      {"LONGVARCHAR","java.lang.String"},
      {"NCHAR","java.lang.String"},
      {"NVARCHAR","java.lang.String"},
      {"LONGNVARCHAR","java.lang.String"},
      {"GRAPHIC","java.lang.String"},
      {"VARGRAPHIC","java.lang.String"},
      {"TEXT","java.lang.String"},
      {"NUMERIC","java.math.BigDecimal"},
      {"DECIMAL","java.math.BigDecimal"},
      {"NUMBER","java.math.BigDecimal"},
      {"BIT","java.lang.Boolean"},
      {"BOOLEAN","java.lang.Boolean"},
      {"TINYINT","java.lang.Integer"},
      {"SMALLINT","java.lang.Integer"},
      {"INTEGER","java.lang.Integer"},
      {"SERIAL","java.lang.Integer"},
      {"SERIAL4","java.lang.Integer"},
      {"INT2","java.lang.Integer"},
      {"INT4","java.lang.Integer"},
      {"BIGINT","java.lang.Long"},
      {"REAL","java.lang.Float"},
      {"FLOAT","java.lang.Double"},
      {"Double","java.lang.Double"},
      {"BINARY","byte[]"},
      {"VARBINARY","byte[]"},
      {"LONGVARBINARY","byte[]"},
      {"DATE","java.sql.Date"},
      {"TIME","java.sql.Time"},
      {"TIMESTAMP","java.sql.Timestamp"},
      {"DISTINCT","java.lang.Object"},
      {"CLOB","java.sql.Clob"},
      {"NCLOB","java.sql.NClob"},
      {"BLOB","java.sql.Blob"},
      {"ARRAY","java.sql.Array"},
      {"STRUCT","java.sql.Struct"},
      {"REF","java.sql.Ref"},
      {"DATALINK","java.net.URL"},
      {"ROWID","java.sql.RowId"},
      {"SQLXML","java.sql.SQLXML"},
      {"MPAA_RATING","java.lang.String"},
      {"_TEXT","java.lang.String[]"},
      {"TSVECTOR","org.postgresql.util.PGobject"},
  }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

}
