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

import org.scribble.ast.global.GProtoDecl;
import org.scribble.codegen.java.sessionapi.SessionApiGenerator;
import org.scribble.codegen.java.statechanapi.ScribSockGen;
import org.scribble.codegen.java.statechanapi.StateChannelApiGenerator;
import org.scribble.codegen.java.util.*;
import org.scribble.core.model.endpoint.EState;
import org.scribble.core.type.name.GProtoName;
import org.scribble.core.type.name.Role;
import org.scribble.util.ScribException;

import java.util.List;

public abstract class ScribMSGen extends ScribSockGen {
    protected final EState curr;
    protected final String className;
    protected final StateMSApiGenerator stateMSApiGenerator;
    public static final String OUTPUTMS_CLASS = "org.scribble.runtime.statechans.SendMS";
    public static final String RECEIVEMS_CLASS = "org.scribble.runtime.statechans.ReceiveMS";
    public static final String BRANCHMS_CLASS = "org.scribble.runtime.statechans.BranchMS";
    public static final String CASEMS_CLASS = "org.scribble.runtime.statechans.CaseMS";
    public static final String ENDMS_CLASS = "org.scribble.runtime.statechans.EndMS";
    public static final String CONNECTION_CLASS = "org.scribble.runtime.session.MSEndpoint";
    public static final String GENERATED_ENDMS_NAME = "EndSocketMS";
    public static final String SCRIBSOCKET_SE_FIELD = JavaBuilder.THIS + ".msEndpoint";

    protected final ClassBuilder cb = new ClassBuilder();

    public ScribMSGen(StateMSApiGenerator apigen, EState curr) {
        super(apigen, curr);
        this.stateMSApiGenerator = apigen;
        this.curr = curr;
        this.className = getClassName();
    }

    protected String getClassName()
    {
        return this.apigen.getMSClassName(this.curr);
    }

    @Override
    public ClassBuilder generateType() throws ScribException {
        constructClass();
        return this.cb;
    }

    protected void constructClass() throws ScribException
    {
        constructClassExceptMethods();
        addMethods();
    }

    protected void constructClassExceptMethods()
    {
        this.cb.setName(this.className);
        this.cb.setPackage(getStateChannelPackageName());
        this.cb.addModifiers(JavaBuilder.PUBLIC, JavaBuilder.FINAL);
        this.cb.setSuperClass(getSuperClassType());
        addImports();
        addConstructor();
    }

    protected abstract String getSuperClassType();

    protected abstract void addMethods() throws ScribException;

    protected void addImports()
    {
        this.cb.addImports("java.io.IOException");
        //this.cb.addImports(getSessionPackageName() + "." + getSessionClassName());
        this.cb.addImports(getEndpointApiRootPackageName() + ".*");
        this.cb.addImports(getRolesPackageName() + ".*");
    }

    protected MethodBuilder addConstructor()
    {
        final String CONNECTION_PAR = "msEndpoint";
        ConstructorBuilder ctor = cb.newConstructor(
                CONNECTION_CLASS + "<" + getSelfClassName() + ">" + " " + CONNECTION_PAR, "boolean dummy");
        ctor.addModifiers(JavaBuilder.PROTECTED);
        ctor.addBodyLine(JavaBuilder.SUPER + "(" + CONNECTION_PAR + ");");
        if (this.curr.equals(this.stateMSApiGenerator.getInitialState())) {
            addInitialStateConstructor();
        }
        return ctor;
    }

    protected void addInitialStateConstructor()
    {
        final String CONNECTION_PAR = "msEndpoint";

        List<Role> roles = this.apigen.getJob().getCore().getContext().getIntermediate(this.apigen.getGProtocolName()).roles;

        ConstructorBuilder ctor2 = cb.newConstructor(
                CONNECTION_CLASS + "<" + getSelfClassName() + ">" + " " + CONNECTION_PAR);

//        ctor2.addExceptions(StateChannelApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS);
        ctor2.addModifiers(JavaBuilder.PUBLIC);
        ctor2.addBodyLine(JavaBuilder.SUPER + "(" + CONNECTION_PAR + ");");
//        ctor2.addBodyLine(CONNECTION_PAR + ".init();");
    }

    protected String getStateChannelPackageName()
    {
        System.out.println(this.apigen.getGProtocolName().toString());
        //return getSessionPackageName() + ".channels." + this.apigen.getSelf();
        return this.apigen.getGProtocolName().toString() + ".ms." + this.apigen.getSelf();
    }

    public static void setNextSocketReturnType(StateChannelApiGenerator apigen, MethodBuilder mb, EState succ)
    {
        String ret;
        if (succ.isTerminal())
        {
            GProtoName gpn = apigen.getGProtocolName();
            Role self = apigen.getSelf();
            ret = SessionApiGenerator.getMSChannelPackageName(gpn, self) + "." + GENERATED_ENDMS_NAME;
            //+ "<" + SessionApiGenerator.getSessionClassName(gpn) + ", " + SessionApiGenerator.getRoleClassName(self) + ">";
        }
        else
        {
            ret = apigen.getMSClassName(succ);
        }
        mb.setReturn(ret);
    }

    protected void addReturnNextSocket(MethodBuilder mb, EState s)
    {
        String nextClass;
        //if (isTerminalClassName(nextClass))
        if (s.isTerminal())
        {
            mb.addBodyLine(SCRIBSOCKET_SE_FIELD + ".setCompleted();");  // Do before the IO action? in case of exception?
            nextClass = GENERATED_ENDMS_NAME;// + "<>";
        }
        else
        {
            nextClass = this.apigen.getMSClassName(s);
            System.out.println(nextClass);
        }
        mb.addBodyLine(JavaBuilder.RETURN + " " + JavaBuilder.NEW + " " + nextClass + "(" + SCRIBSOCKET_SE_FIELD + ", true);");
    }
}
