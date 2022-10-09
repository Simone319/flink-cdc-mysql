package model.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.item.Customer;
import model.item.CustomerOrder;
import model.item.Order;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;

public class AsyncCustomerOrderRequest extends RichAsyncFunction<String, String> {

    private Connection connection;
    private Logger log;

    public AsyncCustomerOrderRequest(Logger log) {
        this.log = log;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        connection = getConnection();
    }

    private Connection getConnection() {
        Connection con = null;
        try {
            log.info("Start to prepare JDBC connection.");
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://my-hostname:port/database",
                    "admin", "password");
            log.info("Successfully prepare JDBC connection.");
        } catch (Exception e) {
            log.info("-----------mysql get connection has exception , msg = "+ e.getMessage());
        }
        return con;
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (connection != null) {// close the connection and release the resource
            connection.close();
        }
    }

    @Override
    public void asyncInvoke(String cdcString, final ResultFuture<String> resultFuture) throws Exception {
        log.info("CDC String received: " + cdcString);
        ObjectMapper mapper = new ObjectMapper();
        if (cdcString.contains("\"op\":\"c\"")) {
            int start = cdcString.indexOf("\"after\":") + 8;
            int end = cdcString.indexOf("}")+ 1;
            String orderString = cdcString.substring(start, end);
            log.info("start: " + start + ", end: " + end + ", orderString: " + orderString);

            Order order = mapper.readValue(orderString, Order.class);

            log.info("Creating statement to query MySQL.");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format("select customerId, sex, age, birthMonth, birthDay, " +
                    "openDays, totalPoints, province, membership from customers where customerId = \"%s\"", order.customerId));
            log.info("Successfully executed the query, result is: " + resultSet);

            if (resultSet != null && resultSet.next()) {
                Customer customer = new Customer(
                        resultSet.getString("customerId"),
                        resultSet.getString("sex"),
                        resultSet.getInt("age"),
                        resultSet.getInt("birthMonth"),
                        resultSet.getInt("birthDay"),
                        resultSet.getInt("openDays"),
                        resultSet.getInt("totalPoints"),
                        resultSet.getString("province"),
                        resultSet.getString("membership")
                );
                CustomerOrder orderCustomerAge = new CustomerOrder(order, customer);
                log.info("orderCustomerAge is: " + orderCustomerAge);
                resultFuture.complete(Collections.singleton(mapper.writeValueAsString(orderCustomerAge)));
            } else {
                log.info("No resultSet is returned, hence returning empty list.");
                resultFuture.complete(Collections.EMPTY_LIST);
            }
        }
        resultFuture.complete(Collections.EMPTY_LIST);
    }
}