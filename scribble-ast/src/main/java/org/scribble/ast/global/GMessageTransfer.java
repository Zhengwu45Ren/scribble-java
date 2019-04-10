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
package org.scribble.ast.global;

import java.util.stream.Collectors;

import org.antlr.runtime.Token;
import org.scribble.ast.MessageTransfer;
import org.scribble.core.type.kind.Global;
import org.scribble.util.Constants;

public class GMessageTransfer extends MessageTransfer<Global>
		implements GSimpleSessionNode
{
	// ScribTreeAdaptor#create constructor
	public GMessageTransfer(Token t)
	{
		super(t);
	}

	// Tree#dupNode constructor
	public GMessageTransfer(GMessageTransfer node)
	{
		super(node);
	}
	
	@Override
	public GMessageTransfer dupNode()
	{
		return new GMessageTransfer(this);
	}

	@Override
	public String toString()
	{
		return getMessageNodeChild() + " " + Constants.FROM_KW
				+ " " + getSourceChild() + " " + Constants.TO_KW
				+ " "
				+ getDestinationChildren().stream().map(x -> x.toString())
						.collect(Collectors.joining(", "))
				+ ";";
	}
}
