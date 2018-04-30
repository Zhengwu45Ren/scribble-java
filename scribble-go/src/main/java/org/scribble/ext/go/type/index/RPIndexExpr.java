package org.scribble.ext.go.type.index;

import java.util.Set;

public abstract class RPIndexExpr
{
	public boolean isConstant()
	{
		return false;
	}
	
	public abstract String toGoString();
	
	public abstract Set<RPIndexVar> getVars();  // Doesn't include foreach params

	public abstract String toSmt2Formula();  // Cf. toString -- but can be useful to separate, for debugging (and printing)

	// N.B. "syntactic" comparison
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof RPIndexExpr))
		{
			return false;
		}
		return ((RPIndexExpr) o).canEqual(this);
	}

	protected abstract boolean canEqual(Object o);
	
	// In case subclasses do super
	@Override
	public int hashCode()
	{
		return 5869;
	}
}
