import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import src.lib.ServiceUtil;

import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserService {
    static JSONObject data = new JSONObject();
    static Connection connection = null;
    static Statement statement = null;

    
    /** 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        // create a database connection
        try{
            connection = DriverManager.getConnection("jdbc:sqlite:compiled/UserService/user.db");
            statement = connection.createStatement();
            // SQL statement for creating a new table
            String sql = "CREATE TABLE IF NOT EXISTS users (\n"
            + "	id integer PRIMARY KEY,\n"
            + "	username varchar(255),\n"
            + "	email varchar(255),\n"
            + "	password varchar(255)\n"
            + ");";
            statement.execute(sql);

            Connection connectionOrder = DriverManager.getConnection("jdbc:sqlite:compiled/UserService/order.db");
            Statement statementOrder = connectionOrder.createStatement();
            // SQL statement for creating a new table
            sql = "CREATE TABLE IF NOT EXISTS orders (\n"
            + "	id integer PRIMARY KEY,\n"
            + "	userid integer,\n"
            + "	productid integer,\n"
            + "	quantity integer\n"
            + ");";
            statementOrder.execute(sql);
        } catch(SQLException e){
          // if the error message is "out of memory",
          // it probably means no database file is found
          System.err.println(e.getMessage());
        }

        //Read config.json
        String path = args[0];
        String jsonString = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Process each line
                jsonString = jsonString.concat(line);
                jsonString = jsonString.replace(" ","");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Map representing config.json
        JSONObject jsonObject = new JSONObject(jsonString);

        int port = jsonObject.getJSONObject("UserService").getInt("port");
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Set up context for /user request
        server.createContext("/user", new UserHandler());



        server.setExecutor(null); // creates a default executor

        server.start();

        System.out.println("Server started on port " + port);
    }

    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            //Print client info for debugging
            ServiceUtil.printClientInfo(exchange);

            // Handle GET request for /user
            JSONObject responseMap = new JSONObject();
            responseMap.put("rcode", "500");
            if ("GET".equals(exchange.getRequestMethod())){
                try {
                    System.out.println("It is a GET request for user");

                    //Get parameter
                    String clientUrl = exchange.getRequestURI().toString();
                    int index = clientUrl.indexOf("user") + "user".length() + 1;
                    String params = clientUrl.substring(index);

                    //Checking if the request is valid
                    if(!ServiceUtil.isNumeric(params)){
                        responseMap.put("rcode", "400");
                    } else{
                        //Execute query
                        makeResponse(responseMap, params, statement);
                    }
                } catch (Exception e) {
                    ServiceUtil.sendResponse(exchange, responseMap);
                    System.out.println(e.getMessage());
                    throw new RuntimeException(e);
                }
            }
            //Handle POST request for /user 
            else if("POST".equals(exchange.getRequestMethod())){
                try {
                    System.out.println("It is a POST request for user");
                    String dataString = ServiceUtil.getRequestBody(exchange);

                    System.out.println("The request body: " + dataString);

                    JSONObject dataMap = ServiceUtil.bodyToMap(dataString);
                    if(ServiceUtil.isJSON(dataString) && ServiceUtil.isValidUser(dataMap)){

                        //Handle create
                        if(dataMap.get("command").equals("create")){
                            System.out.println("Create an entry");
                            if(!ServiceUtil.getQuery("users", dataMap.get("id").toString().toString(), statement).isBeforeFirst()){
                                //Create a new User
                                String command = String.format(
                                                    "INSERT INTO users\n" + 
                                                    "(id, username, email, password)\n" +
                                                    "VALUES\n" +
                                                    "(%s, \'%s\', \'%s\', \'%s\')",
                                                    dataMap.get("id").toString(),
                                                    dataMap.get("username"),
                                                    dataMap.get("email"),
                                                    dataMap.get("password")
                                                );
                                statement.execute(command);
                                makeResponse(responseMap, dataMap.get("id").toString(), statement);
                            } else{
                                //User already exists
                                responseMap.put("rcode", "409");
                            }
                        }

                        //Handle update
                        if(dataMap.get("command").equals("update")){
                            System.out.println("Update an entry");
                            if(ServiceUtil.getQuery("users", dataMap.get("id").toString(), statement).isBeforeFirst()){
                                
                                //Check if the username needs to be updated
                                if(dataMap.has("username")){
                                    ServiceUtil.updateDB("users", "username", dataMap.get("username").toString(), dataMap.get("id").toString(), statement);
                                }

                                //Check if the email needs to be updated
                                if(dataMap.has("email")){
                                    ServiceUtil.updateDB("users", "email", dataMap.get("email").toString(), dataMap.get("id").toString(), statement);
                                }

                                //Check if the password needs to be updated
                                if(dataMap.has("password")){
                                    ServiceUtil.updateDB("users", "password", dataMap.get("password").toString(), dataMap.get("id").toString(), statement);
                                }

                                makeResponse(responseMap, dataMap.get("id").toString(), statement);
                            } else{
                                //User does not exist
                                responseMap.put("rcode", "404");
                            }
                        }

                        //Handle delete
                        if(dataMap.get("command").equals("delete")){
                            System.out.println("Delete an entry");
                            ResultSet resultSet = ServiceUtil.getQuery("users", dataMap.get("id").toString(), statement);
                            if(resultSet.isBeforeFirst()){
                                resultSet.next();
                                //Authenticate
                                if(resultSet.getString("username").equals(dataMap.get("username")) &&
                                    resultSet.getString("email").equals(dataMap.get("email")) &&
                                    resultSet.getString("password").equals(dataMap.get("password"))
                                ){
                                    String command = String.format("DELETE FROM users WHERE id = %s;", dataMap.get("id").toString());
                                    statement.execute(command);
                                    responseMap.put("rcode", "200");
                                } else{
                                    //Authetication failed
                                    responseMap.put("rcode", "404");
                                }
                            } else{
                                //User does not exist
                                responseMap.put("rcode", "404");
                            }
                        }
                    } else{
                        responseMap.put("rcode", 400);
                    }
                    
                } catch (Exception e) {
                    ServiceUtil.sendResponse(exchange, responseMap);
                    System.out.println(e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            ServiceUtil.sendResponse(exchange, responseMap);


        }

    }

    /** 
     * @param responseMap
     * @param params
     * @param statement
     * @throws SQLException
     * @throws NoSuchAlgorithmException
     */
    public static void makeResponse(JSONObject responseMap, String params, Statement statement) throws SQLException, NoSuchAlgorithmException {
        ResultSet result = ServiceUtil.getQuery("users", params, statement);

        //Check if user is found
        if (!result.isBeforeFirst() ) {
            responseMap.put("rcode", "404"); 
        } else{ 
            //Make a response
            responseMap.put("rcode", "200");
            result.next();
            responseMap.put("id", Integer.parseInt(params));
            responseMap.put("username", result.getString("username"));
            responseMap.put("email", result.getString("email"));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(result.getString("password").getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < encodedhash.length; i++) {
            String hex = Integer.toHexString(0xff & encodedhash[i]).toUpperCase();
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
            responseMap.put("password", hexString.toString());
        }
    }

}

