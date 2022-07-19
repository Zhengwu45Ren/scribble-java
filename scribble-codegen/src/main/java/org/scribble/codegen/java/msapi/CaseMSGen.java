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

import org.scribble.ast.Module;
import org.scribble.ast.SigDecl;
import org.scribble.codegen.java.sessionapi.SessionApiGenerator;
import org.scribble.codegen.java.statechanapi.BranchSockGen;
import org.scribble.codegen.java.statechanapi.ReceiveSockGen;
import org.scribble.codegen.java.statechanapi.StateChannelApiGenerator;
import org.scribble.codegen.java.statechanapi.ioifaces.BranchIfaceGen;
import org.scribble.codegen.java.statechanapi.ioifaces.IOStateIfaceGen;
import org.scribble.codegen.java.util.ClassBuilder;
import org.scribble.codegen.java.util.FieldBuilder;
import org.scribble.codegen.java.util.JavaBuilder;
import org.scribble.codegen.java.util.MethodBuilder;
import org.scribble.core.model.endpoint.EState;
import org.scribble.core.model.endpoint.actions.EAction;
import org.scribble.core.type.name.DataName;
import org.scribble.core.type.name.Role;
import org.scribble.core.type.name.SigName;
import org.scribble.util.ScribException;

import java.util.stream.Collectors;

public class CaseMSGen extends ScribMSGen{
    public CaseMSGen(StateMSApiGenerator apigen, EState curr) {
        super(apigen, curr);
    }

    protected String getClassName()
    {
        return getCaseMSName(super.getClassName());
    }

    @Override
    protected String getSuperClassType() {
        return CASEMS_CLASS +  "<" + getSelfClassName() + ">";
    }

    @Override
    protected void addImports()
    {
        super.addImports();
        this.cb.addImports(getOpsPackageName() + ".*");
    }

    @Override
    protected void addInitialStateConstructor()
    {}

    @Override
    protected MethodBuilder addConstructor()
    {
        String branchName = this.apigen.getMSClassName(curr);  // Name of "parent" branch class (curr state is the branch state)
        String enumClassName = branchName + "." + BranchMSGen.getBranchEnumClassName(this.stateMSApiGenerator, this.curr);

        MethodBuilder ctor = super.addConstructor();
        ctor.addParameters(enumClassName + " " + CASE_OP_PARAM, StateChannelApiGenerator.SCRIBMESSAGE_CLASS + " " + CASE_MESSAGE_PARAM);
        ctor.addBodyLine(JavaBuilder.THIS + "." + CASE_OP_FIELD + " = " + CASE_OP_PARAM + ";");
        ctor.addBodyLine(JavaBuilder.THIS + "." + CASE_MESSAGE_FIELD + " = " + CASE_MESSAGE_PARAM + ";");
        return ctor;
    }

    @Override
    protected void addMethods() throws ScribException {
        String branchName = this.stateMSApiGenerator.getMSClassName(curr);  // Name of "parent" branch class (curr state is the branch state)
        String enumClassName = branchName + "." + BranchMSGen.getBranchEnumClassName(this.stateMSApiGenerator, this.curr);
        //String className = newClassName();  // Name of branch-receive class

        FieldBuilder fb1 = this.cb.newField(CASE_OP_FIELD);  // The op enum, for convenient switch/if/etc by user (correctly derived by code generation from the received ScribMessage)
        fb1.addModifiers(JavaBuilder.PUBLIC, JavaBuilder.FINAL);
        fb1.setType(enumClassName);

        FieldBuilder fb2 = this.cb.newField(CASE_MESSAGE_FIELD);  // The received ScribMessage (branch-check checks the user-selected receive op against the ScribMessage op)
        fb2.addModifiers(JavaBuilder.PRIVATE, JavaBuilder.FINAL);
        fb2.setType(StateChannelApiGenerator.SCRIBMESSAGE_CLASS);

        for (EAction a : this.curr.getDetActions())
        {
            EState succ = this.curr.getDetSuccessor(a);
            addReceiveMethod(this.cb, a, succ);
            addCaseReceiveMethod(this.cb, a, succ);
        }

        this.stateMSApiGenerator.addTypeDeclFut(this.cb);
    }

    private MethodBuilder makeReceiveHeader(ClassBuilder cb, EAction a, EState succ) throws ScribException
    {
        MethodBuilder mb = cb.newMethod();
        ReceiveMSGen.setReceiveHeaderWithoutReturnType(this.stateMSApiGenerator, a, mb);
        setNextSocketReturnType(this.stateMSApiGenerator, mb, succ);
        return mb;
    }

    private void addReceiveMethod(ClassBuilder cb, EAction a, EState succ) throws ScribException
    {
        Module main = this.stateMSApiGenerator.getMainModule();

        MethodBuilder mb = makeReceiveHeader(cb, a, succ);
        if (a.mid.isOp())
        {
            addBranchCheck(getSessionApiOpConstant(a.mid), mb, CASE_MESSAGE_FIELD);
            ReceiveMSGen.addPayloadBuffSetters(main, a, mb);
        }
        else //if (a.mid.isMessageSigName())
        {
            SigDecl msd = main.getSigDeclChild(((SigName) a.mid).getSimpleName());  // FIXME: might not belong to main module
            addBranchCheck(getSessionApiOpConstant(a.mid), mb, CASE_MESSAGE_FIELD);
            mb.addBodyLine(CASE_ARG_PREFIX + "." + BUFF_VAL_FIELD + " = (" + msd.getExtName() + ") " + CASE_MESSAGE_FIELD + ";");
        }
        addReturnNextSocket(mb, succ);
    }

    private MethodBuilder makeCaseReceiveHeader(ClassBuilder cb, EAction a, EState succ) throws ScribException
    {
        MethodBuilder mb = cb.newMethod();
        setCaseReceiveHeaderWithoutReturnType(this.stateMSApiGenerator, a, mb);
        setNextSocketReturnType(this.stateMSApiGenerator, mb, succ);
        return mb;
    }

    private void addCaseReceiveMethod(ClassBuilder cb, EAction a, EState succ) throws ScribException
    {
        MethodBuilder mb = makeCaseReceiveHeader(cb, a, succ);
        String ln = JavaBuilder.RETURN + " " + "receive(" + getSessionApiRoleConstant(a.obj) + ", ";
        //ln += mb.getParameters().stream().map((p) -> p.substring(p.indexOf(" ") + 1, p.length())).collect(Collectors.joining(", ")) + ");";
        boolean first = true;
        for (String param : mb.getParameters())
        {
            if (first)
            {
                first = false;
            }
            else
            {
                ln += ", ";
            }
            if (param.contains("<"))
            {
                param = param.substring(param.lastIndexOf('>') + 1, param.length());
            }
            ln += param.substring(param.indexOf(" ") + 1, param.length());
        }
        mb.addBodyLine(ln + ");");
    }

    private static void addBranchCheck(String opClassName, MethodBuilder mb, String messageField)
    {
        String op = JavaBuilder.THIS + "." + messageField + "." + StateChannelApiGenerator.SCRIBMESSAGE_OP_FIELD;
        mb.addBodyLine("if (!" + op + ".equals(" + opClassName + ")) {");
        mb.addBodyLine(1, "throw " + JavaBuilder.NEW + " " + StateChannelApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS + "(\"Wrong branch, received: \" + " + op + ");");
        mb.addBodyLine("}");
    }

    // As for ReceiveSocket, but without peer param
    public static void setCaseReceiveHeaderWithoutReturnType(StateMSApiGenerator apigen, EAction a, MethodBuilder mb) throws ScribException
    {
        //final String ROLE_PARAM = "role";
        Module main = apigen.getMainModule();  // FIXME: main not necessarily the right module?
        String opClass = SessionApiGenerator.getOpClassName(a.mid);

        mb.setName("receive");
        mb.addModifiers(JavaBuilder.PUBLIC);
        mb.addParameters(opClass + " " + StateChannelApiGenerator.RECEIVE_OP_PARAM);  // More params may be added later (payload-arg/future Buffs)
        mb.addExceptions(StateChannelApiGenerator.SCRIBBLERUNTIMEEXCEPTION_CLASS);//, "ExecutionException", "InterruptedException");
        if (a.mid.isOp())
        {
            ReceiveMSGen.addReceiveOpParams(mb, main, a, true);
        }
        else //if (a.mid.isMessageSigName())
        {
            SigDecl msd = main.getSigDeclChild(((SigName) a.mid).getSimpleName());  // FIXME: might not belong to main module
            ReceiveMSGen.addReceiveMessageSigNameParams(mb, msd, true);
        }
    }

    public static String getCaseMSName(String branchSocketName)
    {
        return branchSocketName + "Cases";
    }
}
