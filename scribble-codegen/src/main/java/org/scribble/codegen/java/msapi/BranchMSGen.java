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
import org.scribble.codegen.java.statechanapi.HandlerIfaceGen;
import org.scribble.codegen.java.statechanapi.ScribSockGen;
import org.scribble.codegen.java.statechanapi.StateChannelApiGenerator;
import org.scribble.codegen.java.statechanapi.ioifaces.*;
import org.scribble.codegen.java.util.ClassBuilder;
import org.scribble.codegen.java.util.EnumBuilder;
import org.scribble.codegen.java.util.JavaBuilder;
import org.scribble.codegen.java.util.MethodBuilder;
import org.scribble.core.model.endpoint.EState;
import org.scribble.core.model.endpoint.actions.EAction;
import org.scribble.core.type.name.DataName;
import org.scribble.core.type.name.PayElemType;
import org.scribble.core.type.name.Role;
import org.scribble.core.type.name.SigName;
import org.scribble.util.ScribException;

import java.util.stream.Collectors;

import static org.scribble.codegen.java.statechanapi.ioifaces.IOStateIfaceGen.IOACTION_COMPARATOR;

public class BranchMSGen extends ScribMSGen{
    public BranchMSGen(StateMSApiGenerator apigen, EState curr) {
        super(apigen, curr);
    }

    @Override
    protected String getSuperClassType() {
        return BRANCHMS_CLASS +  "<" + getSelfClassName() + ">";
    }

    @Override
    protected void addImports(){
        this.cb.addImports(
                SessionApiGenerator.getMSChannelPackageName(this.apigen.getGProtocolName(), this.apigen.getSelf())
                        + ".ioifaces.*;");
        super.addImports();
    }

    @Override
    protected void addMethods() throws ScribException
    {
        final String ROLE_PARAM = "role";
        final String MESSAGE_VAR = "m";
        final String OPENUM_VAR = "openum";
        final String OP = MESSAGE_VAR + "." + StateChannelApiGenerator.SCRIBMESSAGE_OP_FIELD;

        Module main = this.stateMSApiGenerator.getMainModule();

        //String next = constructCaseClass(curr, main);
        ClassBuilder caseclass = new CaseMSGen(this.stateMSApiGenerator, this.curr).generateType();
        String next = caseclass.getName();
        String enumClass = getBranchEnumClassName(this.stateMSApiGenerator, this.curr);

        //cb.addImports("java.util.concurrent.ExecutionException");

        //boolean first;
        Role peer = this.curr.getDetActions().iterator().next().obj;

        // Branch method
        addBranchMethod(ROLE_PARAM, MESSAGE_VAR, OPENUM_VAR, OP, peer, next, enumClass);

        //if (IOInterfacesGenerator.skipIOInterfacesGeneration(apigen.getInitialState()))
        if (this.stateMSApiGenerator.skipIOInterfacesGeneration)
        {
            EnumBuilder eb = cb.newMemberEnum(enumClass);
            eb.addModifiers(JavaBuilder.PUBLIC);
            eb.addInterfaces(OPENUM_INTERFACE);
            this.curr.getDetActions().forEach(
                    a -> eb.addValues(SessionApiGenerator.getOpClassName(a.mid)));

//            addDirectBranchCallbackMethod(ROLE_PARAM, MESSAGE_VAR, OP, main, peer);  // Hack: callback apigen while i/o i/f's not supported for connect/accept/etc
        }
        else
        {
            cb.addInterfaces(getBranchInterfaceName(this.apigen.getSelf(), this.curr));
            // Callback methods
//            String handleif = addBranchCallbackMethod(ROLE_PARAM, peer);
//            addHandleInterfaceCallbackMethod(ROLE_PARAM, MESSAGE_VAR, OP, main, peer, handleif);
//            addHandleMethod(ROLE_PARAM, MESSAGE_VAR, OP, main, peer);
        }
    }

    private void addBranchMethod(final String ROLE_PARAM, final String MESSAGE_VAR, final String OPENUM_VAR, final String OP,
                                 Role peer, String next, String enumClass)
    {
        MethodBuilder mb = cb.newMethod("branch");
        mb.setReturn(next);
        mb.addParameters(SessionApiGenerator.getRoleClassName(curr.getDetActions().iterator().next().obj) + " " + ROLE_PARAM);
        mb.addModifiers(JavaBuilder.PUBLIC);

        mb.addBodyLine(StateChannelApiGenerator.SCRIBMESSAGE_CLASS + " " + MESSAGE_VAR + " = "
                + JavaBuilder.SUPER + ".receiveScrib(" + getSessionApiRoleConstant(peer) + ");");
        mb.addBodyLine(enumClass + " " + OPENUM_VAR + ";");
        boolean first = true;
        for (EAction a : this.curr.getDetActions())
        {
            mb.addBodyLine(((first) ? "" : "else ") + "if (" + OP + ".equals(" + getSessionApiOpConstant(a.mid) + ")) {");
            mb.addBodyLine(1, OPENUM_VAR + " = "
                    + enumClass + "." + SessionApiGenerator.getOpClassName(a.mid) + ";");
            mb.addBodyLine("}");
            first = false;
        }
        mb.addBodyLine("else {");
        mb.addBodyLine(1, "throw " + JavaBuilder.NEW + " RuntimeException(\"Won't get here: \" + " + OP + ");");
        mb.addBodyLine("}");
        mb.addBodyLine(JavaBuilder.RETURN + " "
                + JavaBuilder.NEW + " " + next + "(" + SCRIBSOCKET_SE_FIELD + ", true, " + OPENUM_VAR + ", " + MESSAGE_VAR + ");");  // FIXME: dummy boolean not needed
    }

    protected static String getBranchEnumClassName(StateMSApiGenerator apigen, EState curr)
    {
        //return BranchInterfaceGenerator.getBranchInterfaceEnumName(apigen.getSelf(), curr);
        //return (IOInterfacesGenerator.skipIOInterfacesGeneration(apigen.getInitialState()))
        return apigen.skipIOInterfacesGeneration
                ? apigen.getSocketClassName(curr) + "_Enum"
                : BranchInterfaceGen.getBranchInterfaceEnumName(apigen.getSelf(), curr);
    }

    protected static String getBranchInterfaceName(Role self, EState s)
    {
        return "MSBranch_" + self + "_" + s.getDetActions().stream().sorted(IOACTION_COMPARATOR)
                .map(ActionIfaceGen::getActionString).collect(Collectors.joining("__"));
    }
}
