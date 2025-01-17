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
package org.scribble.codegen.java.msapi;

import org.scribble.ast.DataDecl;
import org.scribble.ast.Module;
import org.scribble.ast.SigDecl;
import org.scribble.codegen.java.sessionapi.SessionApiGenerator;
import org.scribble.codegen.java.statechanapi.ScribSockGen;
import org.scribble.codegen.java.util.JavaBuilder;
import org.scribble.codegen.java.util.MethodBuilder;
import org.scribble.core.model.endpoint.EState;
import org.scribble.core.model.endpoint.actions.EAction;
import org.scribble.core.type.name.DataName;
import org.scribble.core.type.name.PayElemType;
import org.scribble.core.type.name.SigName;
import org.scribble.util.ScribException;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SendMSGen extends ScribMSGen{

    public SendMSGen(StateMSApiGenerator apiGen, EState curr) {
        super(apiGen, curr);
    }

    @Override
    protected String getSuperClassType() {
        return OUTPUTMS_CLASS + "<" + getSelfClassName() + ">";
    }

    @Override
    protected void addMethods() throws ScribException{
        final String ROLE_PARAM = "role";

        // Mixed sends and connects
        boolean hasConnect = false;
        boolean hasWrap = false;
        for (EAction a : curr.getDetActions())  // (Scribble ensures all "a" are input or all are output)
        {
            EState succ = curr.getDetSuccessor(a);

            MethodBuilder mb = this.cb.newMethod();
            if (a.isSend())
            {
                setSendHeaderWithoutReturnType(stateMSApiGenerator, a, mb);
            }
            else if (a.isRequest())
            {
                hasConnect = true;
                setConnectHeaderWithoutReturnType(stateMSApiGenerator, a, mb);
            }
            else if (a.isDisconnect())
            {
                setDisconnectHeaderWithoutReturnType(stateMSApiGenerator, a, mb);
            }
            else if (a.isClientWrap())
            {
                hasWrap = true;
                setWrapClientHeaderWithoutReturnType(stateMSApiGenerator, a, mb);
            }
            else
            {
                throw new RuntimeException("[TODO] OutputSocket API generation for: " + a);
            }

            setNextSocketReturnType(this.stateMSApiGenerator, mb, succ);
            if (a.mid.isOp())
            {
                this.cb.addImports(getOpsPackageName() + ".*");  // FIXME: repeated
            }

            if (a.isSend())
            {
                if (a.mid.isOp())
                {
                    List<String> args = getSendPayloadArgs(a);
                    String body = JavaBuilder.SUPER + ".sendScrib(" + ROLE_PARAM + ", " + getSessionApiOpConstant(a.mid);
                    if (!a.payload.isEmpty())
                    {
                        body += ", " + args.stream().collect(Collectors.joining(", "));
                    }
                    body += ");\n";
                    mb.addBodyLine(body);
                }
                else //if (a.mid.isMessageSigName())
                {
                    final String MESSAGE_PARAM = "m";  // FIXME: factor out

                    mb.addBodyLine(JavaBuilder.SUPER + ".sendScrib(" + ROLE_PARAM + ", " + MESSAGE_PARAM + ");");
                }
            }
            else if (a.isRequest())
            {
                //throw new RuntimeException("Shouldn't get in here: " + a);
                mb.addBodyLine(JavaBuilder.SUPER + ".connect(" + ROLE_PARAM + ", cons, host, port);\n");
            }
            else if (a.isDisconnect())
            {
                mb.addBodyLine(JavaBuilder.SUPER + ".disconnect(" + ROLE_PARAM + ");\n");
            }
            else if (a.isClientWrap())
            {
                mb.addBodyLine(JavaBuilder.SUPER + ".wrapClient(" + ROLE_PARAM + ", wrapper);\n");
            }
            else
            {
                throw new RuntimeException("Shouldn't get in here: " + a);
            }

            addReturnNextSocket(mb, succ);
        }
    }

    private static List<String> getSendPayloadArgs(EAction a)
    {
        final String ARG_PREFIX = "arg";

        return IntStream.range(0, a.payload.elems.size()).mapToObj((i) -> ARG_PREFIX + i++).collect(Collectors.toList());  // FIXME: factor out with params
    }

    public static void setSendHeaderWithoutReturnType(StateMSApiGenerator stateMSApiGenerator, EAction a, MethodBuilder mb) throws ScribException
    {
        final String ROLE_PARAM = "role";
        Module main = stateMSApiGenerator.getMainModule();  // FIXME: main not necessarily the right module?

        mb.setName("send");
        mb.addModifiers(JavaBuilder.PUBLIC);
        mb.addExceptions(StateMSApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS, "IOException");
        mb.addParameters(SessionApiGenerator.getRoleClassName(a.obj) + " " + ROLE_PARAM);  // More params added below
        if (a.mid.isOp())
        {
            addSendOpParams(stateMSApiGenerator, mb, main, a);
        }
        else //if (a.mid.isMessageSigName())
        {
            SigDecl msd = main.getSigDeclChild(((SigName) a.mid).getSimpleName());  // FIXME: might not belong to main module
            addSendMessageSigNameParams(mb, msd);
        }
    }

    public static void setConnectHeaderWithoutReturnType(StateMSApiGenerator stateMSApiGenerator, EAction a, MethodBuilder mb)
    {
        final String ROLE_PARAM = "role";

        mb.setName("request");
        mb.addModifiers(JavaBuilder.PUBLIC);
        mb.addExceptions(StateMSApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS, "IOException");
        mb.addParameters(SessionApiGenerator.getRoleClassName(a.obj) + " " + ROLE_PARAM);  // More params added below
        mb.addParameters("Callable<? extends BinaryChannelEndpoint> cons");
        mb.addParameters("String host");
        mb.addParameters("int port");
    }

    public static void setDisconnectHeaderWithoutReturnType(StateMSApiGenerator stateMSApiGenerator, EAction a, MethodBuilder mb)
    {
        final String ROLE_PARAM = "role";

        mb.setName("disconnect");
        mb.addModifiers(JavaBuilder.PUBLIC);
        mb.addExceptions(StateMSApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS, "IOException");
        mb.addParameters(SessionApiGenerator.getRoleClassName(a.obj) + " " + ROLE_PARAM);
    }

    public static void setWrapClientHeaderWithoutReturnType(StateMSApiGenerator stateMSApiGenerator, EAction a, MethodBuilder mb)
    {
        final String ROLE_PARAM = "role";

        mb.setName("wrapClient");
        mb.addModifiers(JavaBuilder.PUBLIC);
        mb.addExceptions(StateMSApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS, "IOException");
        mb.addParameters(SessionApiGenerator.getRoleClassName(a.obj) + " " + ROLE_PARAM);
        mb.addParameters("Callable<? extends BinaryChannelWrapper> wrapper");
    }

    protected static void addSendOpParams(StateMSApiGenerator stateMSApiGenerator, MethodBuilder mb, Module main, EAction a) throws ScribException
    {
        List<String> args = getSendPayloadArgs(a);
        mb.addParameters(SessionApiGenerator.getOpClassName(a.mid) + " op");  // opClass -- op param not actually used in body
        if (!a.payload.isEmpty())
        {
            Iterator<String> as = args.iterator();
            for (PayElemType<?> pt : a.payload.elems)
            {
                if (!pt.isDataName())
                {
                    throw new ScribException("[TODO] API generation not supported for non- data type payloads: " + pt);
                }
                DataDecl dtd = main.getTypeDeclChild((DataName) pt);  // FIXME: might not belong to main module  // TODO: if not DataType
                ScribSockGen.checkJavaDataTypeDecl(dtd);
                mb.addParameters(dtd.getExtName() + " " + as.next());
            }
        }
    }

    protected static void addSendMessageSigNameParams(MethodBuilder mb, SigDecl msd) throws ScribException
    {
        final String MESSAGE_PARAM = "m";

        ScribSockGen.checkMessageSigNameDecl(msd);
        mb.addParameters(msd.getExtName() + " " + MESSAGE_PARAM);
    }
}
