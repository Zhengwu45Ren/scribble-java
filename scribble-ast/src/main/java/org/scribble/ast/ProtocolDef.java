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
package org.scribble.ast;

import org.antlr.runtime.Token;
import org.scribble.core.type.kind.ProtocolKind;
import org.scribble.util.ScribException;
import org.scribble.visit.AstVisitor;

public abstract class ProtocolDef<K extends ProtocolKind> extends ScribNodeBase
		implements ProtocolKindNode<K>
{
	public static final int BLOCK_CHILD_INDEX = 0;

	// ScribTreeAdaptor#create constructor
	public ProtocolDef(Token t)
	{
		super(t);
	}

	// Tree#dupNode constructor
	protected ProtocolDef(ProtocolDef<K> node)
	{
		super(node);
	}
	public abstract ProtocolBlock<K> getBlockChild();
	
	public abstract ProtocolDef<K> dupNode();
	
	public ProtocolDef<K> reconstruct(ProtocolBlock<K> block)
	{
		ProtocolDef<K> pd = dupNode();
		pd.addChild(block);
		pd.setDel(del());  // No copy
		return pd;
	}
	
	@Override
	public ProtocolDef<K> visitChildren(AstVisitor nv) throws ScribException
	{
		ProtocolBlock<K> block = 
				visitChildWithClassEqualityCheck(this, getBlockChild(), nv);
		return reconstruct(block);
	}

	@Override
	public String toString()
	{
		return getBlockChild().toString();
	}
}
