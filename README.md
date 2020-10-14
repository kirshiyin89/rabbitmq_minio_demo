# This project demonstrates how to consume messages from RabbitMQ, sign an ebook based on the received message, and upload the ebook to Minio Cloud storage.

## This project is a demo for my technical article on Medium:
https://medium.com/better-programming/java-process-messages-from-rabbitmq-and-upload-data-to-minio-cloud-b70ecd2e82be

## **Prerequisites**

1. Running Docker container with RabbiqMQ image
2. Running Docker container with Minio image
3. Uploaded alice.epub file to http://localhost:9001/minio/original-ebook/ cloud storage. Use the credentials from MINIO_ACCESS_KEY, MINIO_SECRET_KEY provided in the docker-compose.yml file or replace with your own.

## **How to start this project**

1. Clone this repository.
2. Import the project into your IDE.
3. Run the AppRunner.java to start the application
4. Login to RabbitMQ http://localhost:15672/ using guest/guest credentials
5. Type a name into the payload and hit Publish message
