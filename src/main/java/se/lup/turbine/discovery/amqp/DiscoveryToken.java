/**
 * Copyright 2017 eriklupander
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.lup.turbine.discovery.amqp;

/**
 * Encapsulates a single discovery token message from a hystrix-producer.
 */
public class DiscoveryToken {

    /**
     * String-rep of {@link AmqpInstance.Status}
     */
    private String state;

    /**
     * Typically something like http://some.ip.address:8181/hystrix.stream
     */
    private String address;

    public DiscoveryToken() {
    }

    public DiscoveryToken(String state, String address) {
        this.state = state;
        this.address = address;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
