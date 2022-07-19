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
package org.scribble.runtime.statechans;

import org.scribble.core.type.name.Op;
import org.scribble.core.type.name.Role;
import org.scribble.runtime.message.ScribMessage;
import org.scribble.runtime.session.MSConnect;
import org.scribble.runtime.session.MSEndpoint;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

public class SendMS<R extends Role> extends MSConnect<R> {
    protected SendMS(){}

    protected SendMS(MSEndpoint<R> msEndpoint) {
        super(msEndpoint);
    }

    public void sendScrib(Role peer, Op op, Object... payload){
        try {
            ObjectMessage objectMessage = this.msEndpoint.session.createObjectMessage(new ScribMessage(op, payload));
            this.msEndpoint.producer.send(objectMessage);
        } catch (JMSException e) {
            throw new RuntimeException("Error creating message", e);
        }
    }
}
