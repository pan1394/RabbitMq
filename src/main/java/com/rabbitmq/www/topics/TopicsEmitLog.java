package com.rabbitmq.www.topics;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory; 

public class TopicsEmitLog {
    public final static String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws IOException, TimeoutException {
        //创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        //设置RabbitMQ相关信息
        factory.setHost("192.168.0.105");
        factory.setUsername("admin");
        factory.setPassword("admin"); 
        //创建一个新的连接
        Connection connection = factory.newConnection();
        //创建一个通道
        Channel channel = connection.createChannel();
        //  声明一个队列       
       //  boolean durable = true;
       // channel.queueDeclare(QUEUE_NAME, durable, false, false, null);
        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        String routingKey = getSeverity(args);
        String message = getMessage(args);
        //发送消息到队列中
        channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
        System.out.println(" [x] Sent '" + message + "'"); 
        //关闭通道和连接
        
        channel.close();
        connection.close();
    }
      
    
    private static String getSeverity(String[] strings) {
    	if (strings.length < 1)
            return "info";
    	else
    		return strings[0];
	}


	private static String getMessage(String[] strings){
        if (strings.length < 1)
            return "info";
        return joinStrings(strings, " ");
    }

    private static String joinStrings(String[] strings, String delimiter) {
        int length = strings.length;
        if (length == 0) return "";
        StringBuilder words = new StringBuilder(strings[0]);
        for (int i = 1; i < length; i++) {
            words.append(delimiter).append(strings[i]);
        }
        return words.toString();
    }
}