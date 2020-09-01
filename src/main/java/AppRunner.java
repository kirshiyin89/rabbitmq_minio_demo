import consumer.MessageReceiver;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

class AppRunner {

    public static void main(String[] args) {

        MessageReceiver messageReceiver = new MessageReceiver();
        try {
            messageReceiver.startWork();
        } catch (IOException | TimeoutException | InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }
}
