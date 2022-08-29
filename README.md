# SQL Utils
A selection of utility classes that make it easier to collect the result of an execution of a 
[PreparedStatement](https://docs.oracle.com/javase/7/docs/api/java/sql/PreparedStatement.html).
The database object types are automatically converted to their corresponding Java Objects and their
type is preserved in [SelectResult](src/main/java/com/kmarinos/sqlutils/sql/SelectResult.java).

## How to use

1. Use the [SQLClientFactory](src/main/java/com/kmarinos/sqlutils/sql/SQLClientFactory.java) to create
an [SQLClient](src/main/java/com/kmarinos/sqlutils/sql/SQLClient.java) by providing a valid JDBC-Url, username and password.
2. Call the select() method with a valid sql statement
3. Call one of the terminating methods (see examples below) to collect the result or transform it, one line at a time.

## Examples
For all the examples the following table was created in the database

|  film_id  |        title         |  release_year  |  replacement_cost  |         last_update         |
|:---------:|:--------------------:|:--------------:|:------------------:|:---------------------------:|
|     1     |  "Academy Dinosaur"  |      2006      |       20.99        |  "2013-05-26 14:50:58.951"  |
|     2     |   "Ace Goldfinger"   |      2006      |       12.99        |  "2013-05-26 14:50:58.951"  |
|     3     |  "Adaptation Holes"  |      2006      |       18.99        |  "2013-05-26 14:50:58.951"  |
|     4     |  "Affair Prejudice"  |      2006      |       26.99        |  "2013-05-26 14:50:58.951"  |
|     5     |    "African Egg"     |      2006      |       22.99        |  "2013-05-26 14:50:58.951"  |
|     6     |    "Agent Truman"    |      2006      |       17.99        |  "2013-05-26 14:50:58.951"  |
|     7     |  "Airplane Sierra"   |      2006      |       28.99        |  "2013-05-26 14:50:58.951"  |
|     8     |  "Airport Pollock"   |      2006      |       15.99        |  "2013-05-26 14:50:58.951"  |
|     9     |   "Alabama Devil"    |      2006      |       21.99        |  "2013-05-26 14:50:58.951"  |
|    10     |  "Aladdin Calendar"  |      2006      |       24.99        |  "2013-05-26 14:50:58.951"  |
Additionally, the Java Class [Film.java](src/main/java/com/kmarinos/sqlutils/examples/Film.java) was created
to map the sql results.

### Initialize a client
```java
SQLClient client = SQLClientFactory.connectTo(jdbcUrl, username, password);
```

### Example 1
Get the result of the select statement as a list
```java
List<SelectResult> rows = sqlClient.select(sql).andGet();
```
Each row contains the column names and the values for that row. You can access a value either by column name
```java
String title = rows.get(0).get("title");
```
or all of them at once in a Map<String,Object>
```java
Map<String,Object> values = rows.get(0).getAll();
```

### Example 2a
Collect the result in a List&lt;Film&gt; by mapping each column to an attribute in the class Film
```java
List<Film> films = sqlClient.select(sql).andCollect(row -> {
    var film = new Film();
    film.setTitle(row.get("title"));
    film.setReleaseYear(row.get("release_year"));
    film.setReplacementCost(row.get("replacement_cost"));
    film.setLastUpdate(row.get("last_update"));
    return film;
});
```
### Example 2b
If the names of the columns are the same as the name of the attributes of the mapped class,
the values can be assigned automatically through reflection. Here camel case and snake case are equivalent.

```java
List<Film> films = sqlClient.select(sql).andGetAs(Film.class);
```

### Example 3a
If a reference to the result of the select statement is not needed, you can manipulate the result
with a [Consumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html).
```java
sqlClient.select(sql).andConsume(row->{
  System.out.println("Title:" + row.get("title"));
});
```
### Example 3b
Optionally you can map to a class and then consume it
```java
List<Film> films = new ArrayList<>();
sqlClient.select(sql).andConsumeAs(Film.class, films::add);
```

### Example 4
If your SQL statement contains parameters, you can use the method withParams() to set their values safely.
```java
String sql = "select * from film where film_id > ?";

List<Film> films = sqlClient.select(sql).withParam(5).andGetAs(Film.class);
```

## Licence
[MIT](https://choosealicense.com/licenses/mit/)