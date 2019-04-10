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
package org.scribble.core.job;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.core.lang.global.GProtocol;
import org.scribble.core.lang.local.LProtocol;
import org.scribble.core.model.endpoint.AutParser;
import org.scribble.core.model.endpoint.EGraph;
import org.scribble.core.model.global.SGraph;
import org.scribble.core.type.name.GProtocolName;
import org.scribble.core.type.name.LProtocolName;
import org.scribble.core.type.name.ModuleName;
import org.scribble.core.type.name.Role;
import org.scribble.core.visit.global.InlinedProjector;
import org.scribble.util.ScribException;
import org.scribble.util.ScribUtil;

// Global "static" context information for a Job -- single instance per Job, should not be shared between Jobs
// Mutable: projections, graphs, etc are added mutably later -- replaceModule also mutable setter -- "users" get this from the Job and expect to setter mutate "in place"
public class JobContext
{
	public final Job job;

	// Keys are full names
	// CHECKME: not currently used by core? -- core fully independent of modules, etc., because full disamb already done? (by imed translation)
	//private final Map<ModuleName, ModuleContext> modcs;

	// "Directly" translated global protos, i.e., separate proto decls without any inlining/unfolding/etc
	// Protos retain original decl role list (and args)
  // Keys are full names (though GProtocol already includes full name)
	private final Map<GProtocolName, GProtocol> imeds;

	// Protos have pruned role decls -- CHECKME: prune args?
  // Keys are full names (though GProtocol already includes full name)
	private final Map<GProtocolName, GProtocol> inlined = new HashMap<>();

  // Projected from inlined; keys are full names
	private final Map<LProtocolName, LProtocol> iprojs = new HashMap<>();
	
	// Projected from intermediates
	// LProtocolName is the full local protocol name (module name is the prefix)
	// LProtocolName key is LProtocol value fullname (i.e., redundant)
	private final Map<LProtocolName, LProtocol> projs = new HashMap<>();

	// Built from inlined
	private final Map<LProtocolName, EGraph> fEGraphs = new HashMap<>();
	private final Map<LProtocolName, EGraph> uEGraphs = new HashMap<>();
	private final Map<LProtocolName, EGraph> mEGraphs = new HashMap<>();  
			// Toolchain currently depends on single instance of each graph (state id equality), e.g. cannot re-build or re-minimise, would not be the same graph instance
			// FIXME: currently only minimising "fair" graph, need to consider minimisation orthogonally to fairness -- NO: minimising (of fair) is for API gen only, unfair-transform does not use minimisation (regardless of user flag) for WF

	private final Map<GProtocolName, SGraph> fSGraphs = new HashMap<>();
	private final Map<GProtocolName, SGraph> uSGraphs = new HashMap<>();
	
	protected JobContext(Job job, //Map<ModuleName, ModuleContext> modcs,
			Set<GProtocol> imeds)
	{
		this.job = job;
		//this.modcs = Collections.unmodifiableMap(modcs);
		this.imeds = imeds.stream()
				.collect(Collectors.toMap(x -> x.fullname, x -> x));
	}
	
	// Used by Job for pass running, includes projections (e.g. for reachability checking)
	// Safer to get module names and require user to re-fetch the module by the getter each time (after replacing), to make sure the latest is used
	public Set<ModuleName> getFullModuleNames()
	{
		Set<ModuleName> modnames = new HashSet<>();
		modnames.addAll(getParsedFullModuleNames());
		modnames.addAll(getProjectedFullModuleNames());
		return modnames;
	}

	public Set<ModuleName> getParsedFullModuleNames()
	{
		//return Collections.unmodifiableSet(this.parsed.keySet());
		return this.imeds.keySet().stream().map(x -> x.getPrefix())
				.collect(Collectors.toSet());
	}

	public Set<ModuleName> getProjectedFullModuleNames()
	{
		return this.projs.keySet().stream().map(x -> x.getPrefix())
				.collect(Collectors.toSet());
	}
	
	public GProtocol getIntermediate(GProtocolName fullname)
	{
		return this.imeds.get(fullname);
	}

	public Collection<GProtocol> getIntermediates()
	{
		return this.imeds.values().stream().collect(Collectors.toSet());
	}
	
	public void addInlined(GProtocolName fullname, GProtocol g)
	{
		this.inlined.put(fullname, g);
	}
	
	public GProtocol getInlined(GProtocolName fullname)
	{
		return this.inlined.get(fullname);
	}

	public Set<GProtocol> getInlined()
	{
		return this.inlined.values().stream().collect(Collectors.toSet());
	}
	
	public void addInlinedProjection(LProtocolName fullname, LProtocol l)
	{
		this.iprojs.put(fullname, l);
	}
	
  // Projected from inlined
	public LProtocol getInlinedProjection(GProtocolName fullname, Role self)
	{
		LProtocolName p = InlinedProjector.getFullProjectionName(fullname, self);
		return getInlinedProjection(p);
	}

	public LProtocol getInlinedProjection(LProtocolName fullname)
	{
		return this.iprojs.get(fullname);
	}
	
	public Map<LProtocolName, LProtocol> getInlinedProjections()
	{
		return Collections.unmodifiableMap(this.iprojs);
	}
	
	// Projected from intermediate
	public void addProjection(LProtocol p)
	{
		/*LProtocolName lpn = (LProtocolName) mod.getProtoDeclChildren().get(0)
				.getFullMemberName(mod);
		this.projected.put(lpn, mod);*/
		this.projs.put(p.fullname, p);
	}
	
	public //Module 
			LProtocol getProjection(GProtocolName fullname, Role role)
			throws ScribException
	{
		return getProjection(InlinedProjector.getFullProjectionName(fullname, role));
	}

	public //Module 
			LProtocol getProjection(LProtocolName fullname)
			//throws ScribbleException
	{
		/*Module proj = this.projected.get(fullname);
		if (proj == null)*/
		/*if (!this.projected.containsKey(fullname))
		{
			throw new ScribbleException(
					"Projection not found: " + fullname);
					// E.g. disamb/enabling error before projection passes (e.g. CommandLine -fsm arg)
					//CHECKME: should not occur any more?
		}*/
		return this.projs.get(fullname);
	}
	
	protected void addEGraph(LProtocolName fullname, EGraph graph)
	{
		this.fEGraphs.put(fullname, graph);
	}
	
	// N.B. graphs built from inlined (not unfolded)
	public EGraph getEGraph(GProtocolName fullname, Role role)
			throws ScribException
	{
		LProtocolName fulllpn = InlinedProjector.getFullProjectionName(fullname,
				role);
		// Moved form LProtocolDecl
		EGraph graph = this.fEGraphs.get(fulllpn);
		if (graph == null)
		{
			/*Module proj = getProjection(fullname, role);  // Projected module contains a single protocol
			EGraphBuilder builder = new EGraphBuilder(this.job);  // Obtains an EGraphBuilderUtil from Job
			proj.accept(builder);
			graph = builder.util.finalise();
			addEGraph(fulllpn, graph);*/
			throw new RuntimeException("Shouldn't get in here: ");
		}
		return graph;
	}
	
	protected void addUnfairEGraph(LProtocolName fullname, EGraph graph)
	{
		this.uEGraphs.put(fullname, graph);
	}
	
	public EGraph getUnfairEGraph(GProtocolName fullname, Role role)
			throws ScribException
	{
		LProtocolName fulllpn = InlinedProjector.getFullProjectionName(fullname,
				role);

		EGraph unfair = this.uEGraphs.get(fulllpn);
		if (unfair == null)
		{
			unfair = getEGraph(fullname, role).init
					.unfairTransform(this.job.config.ef).toGraph();
			addUnfairEGraph(fulllpn, unfair);
		}
		return unfair;
	}

	protected void addSGraph(GProtocolName fullname, SGraph graph)
	{
		this.fSGraphs.put(fullname, graph);
	}
	
	public SGraph getSGraph(GProtocolName fullname) throws ScribException
	{
		SGraph graph = this.fSGraphs.get(fullname);
		if (graph == null)
		{
			/*GProtocolDecl gpd = (GProtocolDecl) getModule(fullname.getPrefix())
					.getProtocolDeclChild(fullname.getSimpleName());*/
			Map<Role, EGraph> egraphs = 
					getEGraphsForSGraphBuilding(fullname, //gpd,
							true);
			boolean explicit = //gpd.isExplicit();
					this.imeds.get(fullname).isExplicit();
					//graph = SGraph.buildSGraph(egraphs, explicit, this.job, fullname);
			graph = this.job.buildSGraph(fullname, egraphs, explicit);
			addSGraph(fullname, graph);
		}
		return graph;
	}

	private Map<Role, EGraph> getEGraphsForSGraphBuilding(GProtocolName fullname,
			//GProtocolDecl gpd, 
			boolean fair) throws ScribException
	{
		Map<Role, EGraph> egraphs = new HashMap<>();
		//for (Role self : gpd.getHeaderChild().getRoleDeclListChild().getRoles())
		for (Role self : this.imeds.get(fullname).roles)
		{
			egraphs.put(self, fair 
					? getEGraph(fullname, self) 
					: getUnfairEGraph(fullname, self));
		}
		return egraphs;
	}

	protected void addUnfairSGraph(GProtocolName fullname, SGraph graph)
	{
		this.uSGraphs.put(fullname, graph);
	}

	public SGraph getUnfairSGraph(GProtocolName fullname) throws ScribException
	{
		SGraph graph = this.uSGraphs.get(fullname);
		if (graph == null)
		{
			/*GProtocolDecl gpd = (GProtocolDecl) getModule(fullname.getPrefix())
					.getProtocolDeclChild(fullname.getSimpleName());*/
			Map<Role, EGraph> egraphs = getEGraphsForSGraphBuilding(fullname, //gpd, 
					false);
			boolean explicit = //gpd.isExplicit();
					this.imeds.get(fullname).isExplicit();
			//graph = SGraph.buildSGraph(this.job, fullname, this.job.createInitialSConfig(job, egraphs, explicit));
			graph = this.job.buildSGraph(fullname, egraphs, explicit);
			addUnfairSGraph(fullname, graph);
		}
		return graph;
	}
	
	protected void addMinimisedEGraph(LProtocolName fullname, EGraph graph)
	{
		this.mEGraphs.put(fullname, graph);
	}
	
	public EGraph getMinimisedEGraph(GProtocolName fullname, Role role)
			throws ScribException
	{
		LProtocolName fulllpn = InlinedProjector.getFullProjectionName(fullname,
				role);

		EGraph minimised = this.mEGraphs.get(fulllpn);
		if (minimised == null)
		{
			String aut = runAut(getEGraph(fullname, role).init.toAut(),
					fulllpn + ".aut");
			minimised = new AutParser(this.job).parse(aut);
			addMinimisedEGraph(fulllpn, minimised);
		}
		return minimised;
	}
	
	/*public ModuleContext getModuleContext(ModuleName fullname)
	{
		return this.modcs.get(fullname);
	}*/

	// TODO: relocate
	// Duplicated from CommandLine.runDot
	// Minimises the FSM up to bisimulation
	// N.B. ltsconvert will typically re-number the states
	private static String runAut(String fsm, String aut) throws ScribException
	{
		String tmpName = aut + ".tmp";
		File tmp = new File(tmpName);
		if (tmp.exists())  // Factor out with CommandLine.runDot (file exists check)
		{
			throw new RuntimeException("Cannot overwrite: " + tmpName);
		}
		try
		{
			ScribUtil.writeToFile(tmpName, fsm);
			String[] res = ScribUtil.runProcess("ltsconvert", "-ebisim", "-iaut",
					"-oaut", tmpName);
			if (!res[1].isEmpty())
			{
				throw new RuntimeException(res[1]);
			}
			return res[0];
		}
		finally
		{
			tmp.delete();
		}
	}
}
