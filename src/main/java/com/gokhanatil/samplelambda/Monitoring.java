/*
 * MIT License
 *
 * Copyright (c) 2019 Gokhan Atil (https://gokhanatil.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gokhanatil.samplelambda;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.lambda.runtime.*;
import java.lang.Math;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

public class Monitoring implements RequestHandler<Map<String, Object>, String> {
    public String handleRequest(Map<String, Object> input, Context context) {

        final AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }

        Connection conn;
        Properties connectionProps = new Properties();
        connectionProps.put("user", System.getenv("DB_USER"));
        connectionProps.put("password", System.getenv("DB_PASSWORD"));

        try {
            conn = DriverManager.getConnection("jdbc:oracle:thin:@" + System.getenv("DB_HOSTNAME") + ":"
                    + System.getenv("DB_PORT") + ":" + System.getenv("DB_DATABASE"), connectionProps);

            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT tablespace_name, used_percent FROM dba_tablespace_usage_metrics");
            while (rs.next()) {

                Dimension dimension = new Dimension().withName("Tablespace")
                        .withValue(System.getenv("DB_DATABASE") + ":" + rs.getString(1).toUpperCase());

                MetricDatum datum = new MetricDatum().withMetricName("Space Used (pct)").withUnit(StandardUnit.None)
                        .withValue((double) Math.round(rs.getDouble(2) * 100)).withDimensions(dimension);

                PutMetricDataRequest request = new PutMetricDataRequest().withNamespace("Databases")
                        .withMetricData(datum);

                cw.putMetricData(request);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "{'result': 'success'}";
    }
}
