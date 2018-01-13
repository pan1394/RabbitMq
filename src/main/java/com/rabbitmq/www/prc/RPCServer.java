package com.rabbitmq.www.prc;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RPCServer {
    private final static String TASK_QUEUE_NAME = "prc_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        //设置RabbitMQ地址
        factory.setHost("192.168.0.105");
        factory.setUsername("admin");
        factory.setPassword("admin"); 
        //创建一个新的连接
        Connection connection = null;
        try {
        connection = factory.newConnection();
        //创建一个通道
        final Channel channel = connection.createChannel();
        //声明要关注的队列
        boolean durable = true;
        channel.queueDeclare(TASK_QUEUE_NAME, durable, false, true, null);
        System.out.println(" [x] Awaiting RPC requests");
        channel.basicQos(1);
        //DefaultConsumer类实现了Consumer接口，通过传入一个频道，
        // 告诉服务器我们需要那个频道的消息，如果频道中有消息，就会执行回调函数handleDelivery
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
            	
            	 AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                         .Builder()
                         .correlationId(properties.getCorrelationId())
                         .build();

                 String response = "";

                 try {
                     String message = new String(body,"UTF-8");
                     int n = Integer.parseInt(message);

                     System.out.println(" [.] fib(" + message + ")");
                     response += fib(n);
                 }
                 catch (RuntimeException e){
                     System.out.println(" [.] " + e.toString());
                 }
                 finally {
                     channel.basicPublish( "", properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));

                     channel.basicAck(envelope.getDeliveryTag(), false);

                     // RabbitMq consumer worker thread notifies the RPC server owner thread 
	                 synchronized(this) {
	                     this.notify();
	                 }
                 }
            }
        };
        //自动回复队列应答 -- RabbitMQ中的消息确认机制
        boolean autoAck = false; // Manual Ack
        channel.basicConsume(TASK_QUEUE_NAME, autoAck, consumer);
        
        // Wait and be prepared to consume the message from RPC client.
        while (true) {
            synchronized(consumer) {
	        try {
	              consumer.wait();
	        } catch (InterruptedException e) {
	              e.printStackTrace();          
	        }
            }
        }
        
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null)
        try {
                connection.close();
             } catch (IOException _ignore) {}
        }
}
     
  
    
    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n-1) + fib(n-2);
    }
}