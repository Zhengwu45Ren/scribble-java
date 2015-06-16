package org.scribble2.model.del.global;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble2.model.Continue;
import org.scribble2.model.InteractionNode;
import org.scribble2.model.ModelFactoryImpl;
import org.scribble2.model.ModelNode;
import org.scribble2.model.del.InteractionSeqDel;
import org.scribble2.model.global.GInteractionSeq;
import org.scribble2.model.local.LInteractionNode;
import org.scribble2.model.local.LInteractionSeq;
import org.scribble2.model.local.LocalNode;
import org.scribble2.model.model.ModelAction;
import org.scribble2.model.visit.ModelBuilder;
import org.scribble2.model.visit.Projector;
import org.scribble2.model.visit.env.ModelEnv;
import org.scribble2.model.visit.env.ProjectionEnv;
import org.scribble2.sesstype.kind.Global;
import org.scribble2.sesstype.kind.Local;
import org.scribble2.sesstype.name.Role;
import org.scribble2.util.ScribbleException;


// FIXME: should be a CompoundInteractionDelegate? -- no: compound interaction delegates for typing contexts (done for block only, not seqs)
public class GInteractionSeqDel extends InteractionSeqDel
{
	@Override
	//public Projector enterProjection(ModelNode parent, ModelNode child, Projector proj) throws ScribbleException
	public void enterProjection(ModelNode parent, ModelNode child, Projector proj) throws ScribbleException
	{
		//return (Projector) pushEnv(parent, child, proj);  // Unlike WF-choice and Reachability, Projection uses an Env for InteractionSequences
		pushVisitorEnv(parent, child, proj);  // Unlike WF-choice and Reachability, Projection uses an Env for InteractionSequences
	}
	
	@Override
	public GInteractionSeq leaveProjection(ModelNode parent, ModelNode child, Projector proj, ModelNode visited) throws ScribbleException
	{
		/*LocalInteractionSequence projection = new LocalInteractionSequence(Collections.emptyList());
		ProjectionEnv env = proj.popEnv();
		proj.pushEnv(new ProjectionEnv(env.getJobContext(), env.getModuleDelegate(), projection));
		return (GlobalInteractionSequence) super.leaveProjection(parent, child, proj, visited);*/
		
		GInteractionSeq gis = (GInteractionSeq) visited;
		//List<LocalInteractionNode> lis = new LinkedList<>();
		List<InteractionNode<Local>> lis = new LinkedList<>();
			//this.actions.stream().map((action) -> (LocalInteraction) ((ProjectionEnv) ((LocalNode) action).getEnv()).getProjection()).collect(Collectors.toList());	
		//for (GlobalInteractionNode gi : gis.actions)
		for (InteractionNode<Global> gi : gis.actions)
		{
			LocalNode ln = (LocalNode) ((ProjectionEnv) gi.del().env()).getProjection();
			if (ln instanceof LInteractionSeq)  // Self comm sequence
			{
				lis.addAll(((LInteractionSeq) ln).actions);
			}
			else if (ln != null)
			{
				lis.add((LInteractionNode) ln);
			}
		}
		if (lis.size() == 1)
		{
			if (lis.get(0) instanceof Continue)
			{
				lis.clear();
			}
		}
		LInteractionSeq projection = ModelFactoryImpl.FACTORY.LInteractionSequence(lis);
		ProjectionEnv env = proj.popEnv();
		//proj.pushEnv(new ProjectionEnv(env.getJobContext(), env.getModuleDelegate(), projection));
		proj.pushEnv(new ProjectionEnv(projection));
		//return gis;
		return (GInteractionSeq) popAndSetVisitorEnv(parent, child, proj, gis);  // records the current checker Env to the current del; also pops and merges that env into the parent env
	}
	
	@Override
	public void enterModelBuilding(ModelNode parent, ModelNode child, ModelBuilder builder) throws ScribbleException
	{
		pushVisitorEnv(parent, child, builder);
	}

	@Override
	public GInteractionSeq leaveModelBuilding(ModelNode parent, ModelNode child, ModelBuilder builder, ModelNode visited) throws ScribbleException
	{
		GInteractionSeq gis = (GInteractionSeq) visited;
		Set<ModelAction> all = new HashSet<>();
		Map<Role, ModelAction> leaves = null;
		for (InteractionNode<Global> gi : gis.actions)
		{
			ModelEnv env = ((ModelEnv) gi.del().env());
			Set<ModelAction> as = env.getActions();
			all.addAll(as);
			//Map<Role, ModelAction> tmp = ((ModelEnv) gi.del().env()).getLeaves();
			
			/*Set<ModelAction> tmp = ((ModelEnv) gi.del().env()).getLeaves().values().stream().filter((a) -> !a.getDependencies().isEmpty()).collect(Collectors.toSet());
			for (ModelAction a : tmp)	
			{
				// FIXME: doesn't support self comm
				addDepedency(leaves, a, a.src);
				addDepedency(leaves, a, a.action.peer);
			}*/
			
			if (leaves == null)
			{
				leaves = new HashMap<>(env.getLeaves());
			}
			else
			{
				Set<ModelAction> init = as.stream().filter((a) -> a.getDependencies().isEmpty()).collect(Collectors.toSet());
				addDeps(leaves, init);
				setLeaves(leaves, env.getLeaves().values());
			}
		}

		ModelEnv env = builder.popEnv();
		env = env.setActions(all, leaves);
		builder.pushEnv(env);
		GInteractionSeq tmp = (GInteractionSeq) popAndSetVisitorEnv(parent, child, builder, visited);
		return tmp;
	}
	
	private static void addDeps(Map<Role, ModelAction> leaves, Set<ModelAction> next)
	{
		for (ModelAction a : next)
		{
			if (leaves.containsKey(a.src))
			{
				a.addDependency(leaves.get(a.src));
			}
		}
	}
	
	private static void setLeaves(Map<Role, ModelAction> leaves, Collection<ModelAction> as)
	{
		for (ModelAction a : as)
		{
			leaves.put(a.src, a);
		}
	}
	

	/*private void addDepedency(Map<Role, ModelAction> leaves, ModelAction a, Role r)
	{
		if (!leaves.containsKey(r))
		{
			leaves.put(r, a);
		}
		else
		{
			ModelAction dep = leaves.get(r);
			a.addDependency(dep);
			leaves.put(r, a);
		}
	}*/
}
