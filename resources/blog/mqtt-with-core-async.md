David Nolen has [written](http://swannodette.github.io/2013/07/12/communicating-sequential-processes/) a wonderful article on Clojure's new core.async library and how it eliminates 'callback hell' in the browser.  In a recent [podcast](http://thinkrelevance.com/blog/2013/07/10/rich-hickey-and-core-async-podcast-episode-035), Rich Hickey stresses that callback hell isn't just a problem for JavaScript developers - Java developers, who are increasingly using async APIs, have their fair share of pain too.

In this article I'll try to explain how ```core.async``` helps there too.

To provide some background, I'm working on a client project which is sourcing
[MQTT](http://mqtt.org) (MQ Telemetry Transport) events from sensors and
processing them with [Storm](http://storm-project.net). If you haven't heard of MQTT, its the protocol underlying the 'Internet of Things'.

For my test harness I'm publishing test messages using a Java-based MQTT
API client-library, Eclipse Paho.

When publishing messages using this async API to a broker, the broker
responds by acknowledging delivery. Clients can choose to resend
messages that may have been lost in transit. This is important, because
MQTT is designed to work in harse conditions where the network may be
unreliable.

How does this work?

A device using this Java API connects to a broker by instantiating an instance of ```MqttAsyncClient``` with the url of the broker and a client id.

    client = new MqttAsyncClient("tcp://emergency-room.hospital:1883",
                                  "Malcolm's nose");

To publish a message, the device calls the ```publish``` method with a topic name and a message (some bytes). The method returns a token so that the client can track the delivery of the message.

    IMqttDeliveryToken token = publish(String topic, MqttMessage message);

The tracking is achieved by registering a callback on the client.

    client.setCallback(MqttCallback callback);

The callback interface looks like this :-

    interface MqttCallback {
        void deliveryComplete(IMqttDeliveryToken token);
    }

When a message has been successfully delivered, the token is used to signal this through the ```MqttCallback``` interface. This is pretty simple and not unlike many other async APIs.

Let's imagine that messages usually get through after 10 seconds but if we don't get an acknowledgement we should send the message again. How do we write this?

Normally this would involve keeping track of each message in a data table, along with the time it was sent. Periodically we would have to check the table for any entries that were sent more than 10 seconds ago but where no acknowledgment has been received. This could get tricky, and we'd have to take care not to cause any race conditions between the threads doing the message publication and the thread doing the checks. If we ran the checks too infrequently, we may delay the resend of an important message. If we ran too often, we risk contention and impacting the overall performance of the system.

However, with Clojure's core.async library, life has got a whole lot easier.

To show this, let's switch to writing in Clojure. We publish messages via the Java API in the usual way

    (.publish client "sneezing" (.getBytes "achoo! [80 m/s]"))

This returns our delivery token.

For each token we can create a new ```core.async`` channel (channels, unlike threads, are super light-weight). We'll need to store each token with an associated channel. Let's use an atom for this.

    (def tok->chan (atom {}))

Here's how we associate the token with a channel in the map.

    (let [tok (mqtt-publish client "sneezing" "achoo! [80 m/s]")
          c (chan)]
          (swap! tok->chan assoc tok c))

Now, the point of creating a channel is so our callback interface can handle the
delivery acknowledgments by simply placing something on the channel.

    (reify MqttCallback
      (deliveryComplete [_ tok]
        (when-let [c (get @tok->chan tok)]
          (go (>! c :arrived))
          (swap! chans-by-token dissoc token))))

When an acknowledgement is received, the token is used as a key to look
up the channel. The keyword ```:arrived``` is then 'put' on the channel,
and the channel is removed from the map, as it is no longer needed. Note the put operator is ```>!``` (which must occur within a ```go``` block, don't worry about that yet).

Now, here's the neat trick. We can code the message retry logic in the
_same block of code_ that publishes the original message.

Let's wrap the original message publishing code in loop, so each message
is retried after 10 seconds.

    (go
      (loop []
        (let [tok (mqtt-publish client
                    "sneezing" "achoo! [80 m/s]")
              c (chan)]
          (swap! tok->chan assoc tok c)
          (when-not (alts! c (timeout 10000))
            (recur)))))

The last expression uses ```alts!``` to select over 2 channels. The
first channel, ```c``` is the one we're hoping to take the
```:arrived``` keyword from. Competing in this race is a second channel,
created by the ```timeout``` function, which will close (returning nil)
in 10 seconds. The winner of this race tells us whether the message
arrived ok (if we 'take' ```:arrived``` from ```c```) or nil (if the 10 second countdown expires). If we get a nil, then we go to the top of the loop and try again.
Simple. Magic. Amazing.
