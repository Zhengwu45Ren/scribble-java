/*
 * Copyright 2009-11 www.scribble.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.scribble.protocol.parser.antlr;

import org.antlr.runtime.CommonToken;
import org.scribble.protocol.model.RoleDefn;
import org.scribble.protocol.model.global.GBlock;
import org.scribble.protocol.model.global.GProtocol;

/**
 * This class provides the model adapter for the 'globalProtocolDecl' parser rule.
 *
 */
public class GlobalProtocolDeclModelAdaptor implements ModelAdaptor {

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Object createModelObject(ParserContext context) {
		GProtocol ret=new GProtocol();
		
		ret.setBlock((GBlock)context.pop());
		
		context.pop(); // consume )
		
		ret.getRoleDefinitions().addAll((java.util.List<RoleDefn>)context.pop());
		
		context.pop(); // consume (

		ret.setName(((CommonToken)context.pop()).getText());
		
		context.pop(); // protocol
		context.pop(); // global
		
		context.push(ret);
		
		return ret;
	}

}
