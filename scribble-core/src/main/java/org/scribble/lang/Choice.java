package org.scribble.lang;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.scribble.type.kind.ProtocolKind;
import org.scribble.type.name.Role;

public abstract class Choice<K extends ProtocolKind, B extends Seq<K>>
		extends SessTypeBase<K> implements SessType<K>
{
	public final Role subj;
	public final List<B> blocks;  // Pre: size > 0
			// CHECKME: rename?

	public Choice(org.scribble.ast.Choice<K> source, Role subj,
			List<B> blocks)
	{
		super(source);
		this.subj = subj;
		this.blocks = Collections.unmodifiableList(blocks);
	}

	public abstract Choice<K, B> reconstruct(org.scribble.ast.Choice<K> source, Role subj,
			List<B> blocks);
	
	@Override
	public String toString()
	{
		return "choice at " + this.subj + " "
				+ this.blocks.stream().map(x -> "{\n" + x.toString() + "\n}")
						.collect(Collectors.joining(" or "));
	}

	@Override
	public int hashCode()
	{
		int hash = 1487;
		hash = 31 * hash + super.hashCode();
		hash = 31 * hash + this.subj.hashCode();
		hash = 31 * hash + this.blocks.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Choice))
		{
			return false;
		}
		Choice<?, ?> them = (Choice<?, ?>) o;
		return super.equals(this)  // Does canEquals
				&& this.subj.equals(them.subj) && this.blocks.equals(them.blocks);
	}
}
