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
package org.scribble.codegen.java.statechanapi.ioifaces;

import org.scribble.codegen.java.sessionapi.SessionApiGenerator;
import org.scribble.codegen.java.statechanapi.ScribSockGen;
import org.scribble.codegen.java.statechanapi.StateChannelApiGenerator;
import org.scribble.codegen.java.util.EnumBuilder;
import org.scribble.codegen.java.util.InterfaceBuilder;
import org.scribble.codegen.java.util.JavaBuilder;
import org.scribble.core.model.endpoint.EState;
import org.scribble.core.model.endpoint.actions.EAction;
import org.scribble.core.type.name.GProtoName;
import org.scribble.core.type.name.Role;
import org.scribble.util.ScribException;

import java.util.Map;
import java.util.stream.Collectors;

public class BranchInterfaceGen extends IOStateIfaceGen{
    public BranchInterfaceGen(StateChannelApiGenerator apigen, Map<EAction, InterfaceBuilder> actions, EState curr) {
        super(apigen, actions, curr);
    }

    @Override
    protected void constructInterface() throws ScribException
    {
        super.constructInterface();
        addBranchEnum();
    }

    @Override
    protected void addHeader()
    {
        GProtoName gpn = this.apigen.getGProtocolName();
        Role self = this.apigen.getSelf();
        String packname = getIOInterfacePackageName(gpn, self);
        String ifname = getBranchInterfaceName(self, this.curr);

        this.ib.setName(ifname);
        this.ib.setPackage(packname);
        this.ib.addModifiers(JavaBuilder.PUBLIC);
    }

    protected void addBranchEnum()
    {
        Role self = this.apigen.getSelf();

        // Duplicated from BranchSocketGenerator
        EnumBuilder eb = this.ib.newMemberEnum(getBranchInterfaceEnumName(self, this.curr));
        eb.addModifiers(JavaBuilder.PUBLIC);
        eb.addInterfaces(ScribSockGen.OPENUM_INTERFACE);
        this.curr.getDetActions()
                .forEach(a -> eb.addValues(SessionApiGenerator.getOpClassName(a.mid)));
    }

    // Don't add Action Interfaces (added to CaseInterface)
    @Override
    protected void addSuccessorParamsAndActionInterfaces(){}

    @Override
    protected void addCastField(){}

    public static String getBranchInterfaceEnumName(Role self, EState curr)
    {
        return getIOStateInterfaceName(self, curr) + "_Enum";
    }

    protected static String getIOInterfacePackageName(GProtoName gpn, Role self)
    {
        return SessionApiGenerator.getMSChannelPackageName(gpn, self) + ".ioifaces";
    }

    protected static String getBranchInterfaceName(Role self, EState s)
    {
        return "MSBranch_" + self + "_" + s.getDetActions().stream().sorted(IOACTION_COMPARATOR)
                .map(ActionIfaceGen::getActionString).collect(Collectors.joining("__"));
    }
}
