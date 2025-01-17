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
import org.scribble.codegen.java.sessionapi.SessionApiGenerator;
import org.scribble.codegen.java.statechanapi.*;
import org.scribble.codegen.java.util.ClassBuilder;
import org.scribble.codegen.java.util.TypeBuilder;
import org.scribble.core.job.CoreArgs;
import org.scribble.core.job.CoreContext;
import org.scribble.core.model.endpoint.EState;
import org.scribble.core.model.endpoint.actions.EAction;
import org.scribble.core.type.name.GProtoName;
import org.scribble.core.type.name.LProtoName;
import org.scribble.core.type.name.Role;
import org.scribble.core.visit.global.InlinedProjector;
import org.scribble.job.Job;
import org.scribble.util.ScribException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StateMSApiGenerator extends StateChannelApiGenerator {
    public static final String SCRIBBLERUNTIMEEXCEPTION_CLASS = "org.scribble.main.ScribRuntimeException";
    private final Role self;
    private final LProtoName lpn;
    private final EState init;
    //private final String root;

    protected final boolean skipIOInterfacesGeneration;

    private int counter = 1;
    private Map<EState, String> classNames = new HashMap<>();  // Doesn't include terminal states

    private Map<String, TypeBuilder> types = new HashMap<>();  // class/iface name key

    public StateMSApiGenerator(Job job, GProtoName fullname, Role self)
            throws ScribException // CHECKME: APIGenerationException?
    {
        super(job, fullname, self);

        this.self = self;
        this.lpn = InlinedProjector.getFullProjectionName(fullname, self);
        //this.init = job.getContext().getEndpointGraph(fullname, self).init;
        CoreContext corec = this.core.getContext();
        this.init = this.job.config.args.get(CoreArgs.MIN_EFSM)
                ? corec.getMinimisedEGraph(fullname, self).init
                : corec.getEGraph(fullname, self).init;

        this.skipIOInterfacesGeneration = skipIOInterfacesGeneration(this.init);

        generateClassNames(this.init);
        //this.root = this.classNames.get(this.init);
        constructClasses(this.init);

        //EndpointState term = EndpointState.findTerminalState(new HashSet<>(), this.init);
        EState term = this.init.getTerminal();
        if (term != null)
        {
            ClassBuilder cb = new EndMSGen(this, term).generateType();
            this.types.put(cb.getName(), cb);
        }
    }

    // Cf. IOInterfacesGenerator constructor
    private static boolean skipIOInterfacesGeneration(EState init)
    {
        Set<EAction> as = init.getReachableActions();
        as.addAll(init.getActions());
        if (as.stream().anyMatch(a -> !a.isSend() && !a.isReceive()))  // HACK FIXME (connect/disconnect)
        {
            return true;
        }
        return false;
    }

    // Return: key (package and Java class file path) -> val (Java class source)
    @Override
    public Map<String, String> generateApi()
    {
        Map<String, String> map = new HashMap<String, String>();
        // FIXME: factor out with ScribSocketBuilder.getPackageName
        String prefix = SessionApiGenerator.getEndpointApiRootPackageName(this.gpn)
                .replace('.', '/') + "/ms/" + this.self + "/";
        for (String s : this.types.keySet())
        {
            String path = prefix + s + ".java";
            map.put(path, this.types.get(s).build());
        }
        return map;
    }

    private void generateClassNames(EState ps)
    {
        if (this.classNames.containsKey(ps))
        {
            return;
        }
        if (ps.isTerminal())
        {
            //this.classNames.put(ps, ENDSOCKET_CLASS);  // FIXME: add generic parameters?  or don't record?
            return;
        }
        this.classNames.put(ps, newSocketClassName().replace("_", "") + "MS");
        for (EState succ : ps.getSuccs())
        {
            generateClassNames(succ);
        }
    }

    private String newSocketClassName()
    {
        return this.lpn.getSimpleName().toString() +  "_" + this.counter++;
    }

    private void constructClasses(EState curr) throws ScribException
    {
        if (curr.isTerminal())
        {
            return;  // Generic EndSocket for terminal states
        }
        String className = this.classNames.get(curr);
        if (this.types.containsKey(className))
        {
            return;
        }
        this.types.put(className, constructClass(curr));
        for (EState succ : curr.getSuccs())
        {
            constructClasses(succ);
        }

        // Depends on the above being done first (for this.root)
		/*String init = this.lpn.getSimpleName().toString() + "_" + 0;  // FIXME: factor out with newClassName
		this.classes.put(init, constructInitClass(init));*/
    }

    // Pre: curr is not terminal state
    private ClassBuilder constructClass(EState curr) throws ScribException
    {
        switch (curr.getStateKind())
        {
            case OUTPUT:
            {
                return new SendMSGen(this, curr).generateType();
            }
            case ACCEPT:
            {
                // TODO: WHat is Accept?
                return new AcceptSockGen(this, curr).generateType();
            }
            case UNARY_RECEIVE:
            {
                return new ReceiveMSGen(this, curr).generateType();
            }
            case POLY_RECIEVE:
            {
                // Receive only
                return new BranchMSGen(this, curr).generateType();
            }
            default:
            {
                throw new RuntimeException(
                        "[TODO] State Channel API generation not supported for: "
                                + curr.getStateKind() + ", " + curr.toVerboseString());
            }
        }
    }

    protected EState getInitialState()
    {
        return this.init;
    }

    protected Module getMainModule()
    {
        return this.job.getContext().getMainModule();
    }

    protected void addTypeDeclFut(TypeBuilder tb)
    {
        this.types.put(tb.getName(), tb);
    }
}
