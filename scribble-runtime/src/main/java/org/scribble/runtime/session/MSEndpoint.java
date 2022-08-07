/**
 * Copyright 2008 The Scribble Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.scribble.runtime.session;

import org.scribble.core.type.name.Role;
import org.scribble.main.ScribRuntimeException;

import javax.jms.*;
import javax.jms.Session;
import java.util.HashMap;
import java.util.Map;

public class MSEndpoint<R extends Role>{
    public Connection connection;
    public javax.jms.Session session;
    public MessageProducer producer;
    public Map<String, MessageConsumer> roleConsumerMap;
    private boolean complete = false;
    private boolean closed = false;
    private final Role self;

    public MSEndpoint(R self, Connection connection){
        this.connection = connection;
        try {
            this.self = self;
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(self.toString());
            this.producer = session.createProducer(destination);
            this.roleConsumerMap = new HashMap<>();
        } catch (JMSException e) {
            throw new RuntimeException("Error when initiate connection", e);
        }
    }

    public void connect(Role role){
        try {
            Destination destination = session.createQueue(role.toString());
            this.roleConsumerMap.put(role.toString(),
                    session.createConsumer(destination,
                            "Role = '" + self.toString() + "'"));
        } catch (JMSException e) {
            throw new RuntimeException("Error when initiate connection", e);
        }
    }

    public void setCompleted(){
        this.complete = true;
    }

    public synchronized void close() throws ScribRuntimeException {
        if(!this.closed) {
            try {
                this.closed = true;
                this.producer.close();
                for (MessageConsumer each : this.roleConsumerMap.values()) {
                    each.close();
                }
                this.roleConsumerMap.clear();
                this.session.close();
                this.connection.close();
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }finally {
                if (!this.complete)  // Subsumes use -- must be used for sess to be completed
                {
                    throw new ScribRuntimeException("MS not completed: " + this.self);
                }
            }
        }
    }

}
