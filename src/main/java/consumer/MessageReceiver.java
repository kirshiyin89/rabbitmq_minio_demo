package consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import ebook.EBookHandler;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import nl.siegmann.epublib.domain.Book;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MessageReceiver {

    private static final String QUEUE_INPUT = "my.simple.queue_input";

    private Connection createConnection() throws TimeoutException, InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        // "guest"/"guest" by default, limited to localhost connections
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        return connectToRabbitMQ(factory);
    }

    private Connection connectToRabbitMQ(ConnectionFactory factory) throws TimeoutException, InterruptedException {
        Connection conn = null;
        // if the connection has not been established, retry connection
        for (int retryNum = 100; retryNum >= 0; retryNum--) {
            try {
                conn = factory.newConnection("rabbitmq_connection");
                break;
            } catch (IOException e) {
                System.out.println("Waiting for Rabbitmq connection...");
                Thread.sleep(5000);
            }
        }
        if (conn == null){
            throw new IllegalStateException("Couldn't establish connection to rabbitMQ");
        }
        return conn;
    }

    private void prepareRabbitQueue(Channel channel) throws IOException {
        int prefetchCount = 1;
        channel.basicQos(prefetchCount);
        channel.queueDeclare(QUEUE_INPUT, true, false, false, null);
    }

    public void startWork() throws IOException, TimeoutException, InterruptedException {

        Channel channel = createConnection().createChannel();

        prepareRabbitQueue(channel);

        System.out.println("Waiting for messages...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");

            String filename = UUID.randomUUID() + ".epub";

            // IMPORTANT: it is a prerequisite to create a bucket named original-ebook and upload the alice.epub file to it.
            Book eBook = EBookHandler.openAndSignBook(downloadOriginalBookAsStream(), filename, message);
            if (eBook != null) {
                System.out.println("eBook created " + filename);
                handleFileUpload(filename);
            }
        };
        channel.basicConsume(QUEUE_INPUT, true, deliverCallback, consumerTag -> {
        });
    }

    private InputStream downloadOriginalBookAsStream(){
        InputStream stream;
        try {
            stream = getMinioClient().getObject(
                    GetObjectArgs.builder()
                            .bucket("original-ebook")
                            .object("alice.epub")
                            .build());
        }catch (InvalidKeyException | NoSuchAlgorithmException | ErrorResponseException | InvalidResponseException | InvalidBucketNameException |
                    ServerException | XmlParserException | InsufficientDataException |
                    InternalException | IOException e) {
                System.err.println(e.getMessage());
                throw new IllegalArgumentException("The original ebook file was not found");
            }
        return stream;
    }

    private MinioClient getMinioClient() {

        return MinioClient.builder()
                .endpoint("localhost", 9001, false)
                .credentials("minio", "minio123")
                .build();
    }

    private void handleFileUpload(String filename) {

        MinioClient minioClient = getMinioClient();
        try {

            ObjectWriteResponse response = createBucketAndUploadFile(minioClient, filename);
            if (response != null) {
                String url = createURL(minioClient, filename);
                System.out.println("Created url: " + url);
            }

        } catch (InvalidKeyException | NoSuchAlgorithmException | ErrorResponseException | InvalidResponseException | InvalidBucketNameException |
                ServerException | RegionConflictException | InvalidExpiresRangeException | XmlParserException | InsufficientDataException |
                InternalException | IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private ObjectWriteResponse createBucketAndUploadFile(MinioClient minioClient, String filename) throws
            IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException, RegionConflictException {

        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket("ebookcreator").build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket("ebookcreator").build());
        }
        return minioClient.uploadObject(UploadObjectArgs.builder()
                .bucket("ebookcreator")
                .object(filename)
                .filename(filename)
                .contentType("application/epub")
                .build());
    }

    private String createURL(MinioClient minioClient, String filename) throws
            IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, InvalidExpiresRangeException, ServerException, InternalException, NoSuchAlgorithmException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket("ebookcreator")
                        .object(filename)
                        .expiry(2, TimeUnit.HOURS)
                        .build());
    }
}