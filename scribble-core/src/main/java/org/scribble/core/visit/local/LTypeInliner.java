package org.scribble.core.visit.local;

import org.scribble.core.job.Job;
import org.scribble.core.lang.SubprotoSig;
import org.scribble.core.lang.local.LProtocol;
import org.scribble.core.type.kind.Local;
import org.scribble.core.type.name.LProtocolName;
import org.scribble.core.type.name.ProtocolName;
import org.scribble.core.type.name.RecVar;
import org.scribble.core.type.name.Substitutions;
import org.scribble.core.type.session.Do;
import org.scribble.core.type.session.local.LContinue;
import org.scribble.core.type.session.local.LRecursion;
import org.scribble.core.type.session.local.LSeq;
import org.scribble.core.type.session.local.LType;
import org.scribble.core.visit.STypeInliner;

public class LTypeInliner extends STypeInliner<Local, LSeq>
{
	public LTypeInliner(Job job)
	{
		super(job);
	}

	@Override
	public LType visitDo(Do<Local, LSeq, ? extends ProtocolName<Local>> n)
	{
		LProtocolName fullname = (LProtocolName) n.proto;
		SubprotoSig sig = new SubprotoSig(fullname, n.roles, n.args);
		RecVar rv = getInlinedRecVar(sig);
		if (hasSig(sig))
		{
			return new LContinue(n.getSource(), rv);
		}
		pushSig(sig);
		LProtocol p = this.job.getContext().getProjection(fullname);  // This line differs from GDo version
		Substitutions subs = 
				new Substitutions(p.roles, n.roles, p.params, n.args);
		LSeq inlined = p.def.substitute(subs).visitWith(this);
				// i.e. returning a Seq -- rely on parent Seq to inline
		popSig();
		return new LRecursion(null, rv, inlined);
	}
}
