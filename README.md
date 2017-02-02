# Netflix Turbine w/ AMQP discovery
_Please note:_ Netflix inc. has no affiliation with this plugin/application whatsoever.

_Please note 2:_ This is work-in-progress code used strictly for some demoing purposes. Not production tested.

### What's this?
This is a packaging of the core Netflix Turbine 2.0.0 with a custom AMQP-based service discovery mechanism.

It basically wraps the _turbine-core_ artifact, producing an executable jar that will launch the standard turbine-core with custom service discovery based on AMQP messaging.

Your hystrix producers (e.g. applications publishing hystrix metrics at for example http://IP:8181/hystrix.stream) needs to notify the Turbine stream aggregator of their existence somehow. In the case of standard Netflix Turbine, [Netflix Eureka](https://github.com/Netflix/eureka) is used for this purpose. However, there may be cases where you don't want to use a dedicated Service Discovery mechanism and rather rely on something such as the _service_ abstraction introduced in Docker Swarm mode in Docker 1.12.

This project enables producers to publish their existence to Turbine using plain AMQP messaging. It has only been tested with RabbitMQ as messaging provider, but I guess any AMQP compatible message broker will suffice.

### Building
You probably need to use the supplied gradle wrapper:

    ./gradlew shadowJar

This produces a "fat" executable jar file in the build/libs folder.
   
### Running with default settings

    java -jar build/libs/turbine-amqp-executable-2.0.0-DP.3.jar
    
### Running with custom settings

There are (currently) four custom JVM args one can pass to this customized Turbine:

- amqp.broker.url: Connection string to your AMQP broker. For rabbitmq, the format used is _amqp://username:password@host:port_
- discovery.queue: Name of the AMQP queue to subscribe to
- cluster.name: Name of the cluster, used internally by Turbine.
- consumer.tag: Provides a consumer tag to the message broker. Shouldn't matter much...

Example using all arguments:

    java -jar -Damqp.broker.url=amqp://guest:guest@192.168.99.100:5672 -Ddiscovery.queue=dicovery -Dcluster.name=swarm -Dconsumer.tag=myConsumerTag build/libs/turbine-amqp-executable-2.0.0-DP.3.jar
    
### Client
I developed this plugin in order to use Turbine with Go-based microservices running on Docker Swarm. 

The basic premise is that hystrix stream producers should send a "Discovery Token" every 30 seconds or so that will be consumed by Turbine. The message format selected for these tokens is JSON and the content is extremely simple:

- state: UP or DOWN
- address: IP-address of the producer

Example message: 
    
    {"state": "UP", "address": "192.168.99.104:8181/hystrix.stream"}

Here's the Go code that sends these discovery tokens over AMQP using the [github.com/streadway/amqp](https://github.com/streadway/amqp) library:

    // Defines a struct with json serialization type info.
    type DiscoveryToken struct {
    	State   string `json:"state"` // UP, DOWN?
    	Address string `json:"address"`
    } 

    // Connection object initialized and configured somewhere else...
    func startSendingDiscoveryTokens(conn *amqp.Connection) {
    	token := DiscoveryToken{
    		State:   "UP",
    		Address: GetLocalIP(),
    	}
    	json, _ := json.Marshal(token)
    	go func() {
    		for {
    			sendMessage("discovery", conn, json)
    			time.Sleep(time.Second * 30)
    		}
    	}()
    }
    
    // Simplified (e.g. no error handling or connection checking) code that sends a message.
    func sendMessage(channel string, conn *amqp.Connection, body []byte) {
    	ch, _ := conn.Channel()
    	q, _ := ch.QueueDeclare(
    		channel, // name
    		false,   // durable
    		false,   // delete when unused
    		false,   // exclusive
    		false,   // no-wait
    		nil,     // arguments
    	)
    	ch.Publish(
    		"",     // exchange
    		q.Name, // routing key
    		false,  // mandatory
    		false,  // immediate
    		amqp.Publishing{
    			ContentType: "application/json",
    			Body:        body,
    		})
    }
    
Please note that the code above is just for demonstration purposes...

# LICENSE
Apache License 2.0 see, [LICENSE.md](LICENSE.md)