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
package org.scribble.core.visit;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.scribble.core.type.kind.Local;
import org.scribble.core.type.name.ProtoName;
import org.scribble.core.type.name.RecVar;
import org.scribble.core.type.name.Role;
import org.scribble.core.type.session.Choice;
import org.scribble.core.type.session.Continue;
import org.scribble.core.type.session.DirectedInteraction;
import org.scribble.core.type.session.Do;
import org.scribble.core.type.session.Recursion;
import org.scribble.core.type.session.SType;
import org.scribble.core.type.session.local.LSeq;

// Post: Optional<Role> return is populated for "well-formed" locals, e.g., projections from WF globals
// Return Optional(null) for "failed" inference (n.b., cf. !isPresent, for an "empty" local)
@Deprecated
public class InlinedEnablerInferer
		extends STypeAggNoThrow<Local, LSeq, Optional<Role>>
{
	private Map<RecVar, Optional<Role>> recs;

	public InlinedEnablerInferer(Map<RecVar, Optional<Role>> recs)
	{
		this.recs = Collections.unmodifiableMap(recs);
	}

	@Override
	public Optional<Role> visitChoice(Choice<Local, LSeq> n)
	{
		List<Optional<Role>> enablers = n.blocks.stream().map(x -> visitSeq(x))
				.collect(Collectors.toList());  // Each elem is null, empty or isPresent
		if (enablers.stream().anyMatch(x -> x.isPresent() && x.get() == null))
		{
			return Optional.of(null);
		}
		else if (enablers.stream().allMatch(x -> !x.isPresent()))
		{
			return Optional.empty();
					// CHECKME: can enablers be empty? i.e., choice was "empty" somehow?
					// Yes, when not assuming, e.g., empty block filtering by InlinedProject
		}
		else if (enablers.stream().allMatch(x -> x.isPresent()))
		{
			Set<Role> rs = enablers.stream().map(x -> x.get())
					.collect(Collectors.toSet());
			return (rs.size() > 1) ? Optional.of(null)  
					: Optional.of(rs.iterator().next());
		}
		else
		{
			return Optional.of(null);
		}
	}

	@Override
	public Optional<Role> visitContinue(Continue<Local, LSeq> n)
	{
		return this.recs.containsKey(n.recvar)
				? this.recs.get(n.recvar)
				: Optional.empty();  
						// Corner case, e.g., bad sequence after unguarded continue (will be caught be reachability, e.g., bad.reach.globals.gdo.Test04)
						// Empty allows (bad) Seq to continue to next element
	}

	@Override
	public Optional<Role> visitDirectedInteraction(
			DirectedInteraction<Local, LSeq> n)
	{
		return Optional.of(n.src);
	}

	@Override
	public <N extends ProtoName<Local>> Optional<Role> visitDo(
			Do<Local, LSeq, N> n)
	{
		throw new RuntimeException("Unsupported for Do: " + n);
	}

	public Optional<Role> visitRecursion(Recursion<Local, LSeq> n)
	{
		return visitSeq(n.body);  // null, empty or OK
	}

	@Override
	public Optional<Role> visitSeq(LSeq n)
	{
		for (SType<Local, LSeq> e : n.elems)
		{
			Optional<Role> res = e.visitWithNoThrow(this);
			if (res.isPresent())
			{
				return res;
			}
		}
		return Optional.empty();
	}

	@Override
	protected Optional<Role> unit(SType<Local, LSeq> n)
	{
		return Optional.of(null);  // null signifies inference has "failed" (cf. Stream.of()) -- e.g., encountering a disconnect before anything else
	}

	// N.B. currently only used by visitRecursion, Choice/Seq manually overridden above
	@Override
	protected Optional<Role> agg(SType<Local, LSeq> n, Stream<Optional<Role>> ts)
	{
		throw new RuntimeException("Unsupported: ");
		//return ts.findFirst().get();  // FIXME: should be as in Choice
		/*List<Optional<Role>> tmp = ts.collect(Collectors.toList());
		return (tmp.size() > 1)
				? Optional.of(null)  // Currently doesn't happen, because Choice handled "manually" above
				: tmp.get(0);*/
				// Contents may be null (inference failed)
	}
}
