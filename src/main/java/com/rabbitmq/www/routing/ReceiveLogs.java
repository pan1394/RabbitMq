package com.rabbitmq.www.routing;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class ReceiveLogs {
    private final static String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        //设置RabbitMQ地址
        factory.setHost("192.168.0.105");
        factory.setUsername("admin");
        factory.setPassword("admin"); 
        //创建一个新的连接
        Connection connection = factory.newConnection();
        //创建一个通道
        final Channel channel = connection.createChannel();
        //声明要关注的队列
        //boolean durable = true;
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");
        String queueName = channel.queueDeclare().getQueue();
        if (args.length < 1){
            System.err.println("Usage: ReceiveLogsDirect [info] [warning] [error]");
            System.exit(1);
        }

        for(String severity : args){
            channel.queueBind(queueName, EXCHANGE_NAME, severity);
        }
        System.out.println("Customer Waiting Received messages");
        channel.basicQos(1);
        //DefaultConsumer类实现了Consumer接口，通过传入一个频道，
        // 告诉服务器我们需要那个频道的消息，如果频道中有消息，就会执行回调函数handleDelivery
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
            	try {
            		String message = new String(body, "UTF-8");
            		doWork(message);
            	} catch (InterruptedException e) { 
					e.printStackTrace();
				}finally{ 
            	    channel.basicAck(envelope.getDeliveryTag(), false);
            	} 
            }
        };
        //自动回复队列应答 -- RabbitMQ中的消息确认机制
        boolean autoAck = false; // Manual Ack
        channel.basicConsume(queueName, autoAck, consumer);
    }
    
    private static void doWork(String task) throws InterruptedException {
        for (char ch: task.toCharArray()) {
            if (ch == '.') Thread.sleep(1000);
        }
        System.out.println(" [x] Done, Received:" + task  ); 
    }   
}