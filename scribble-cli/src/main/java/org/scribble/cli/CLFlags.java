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
package org.scribble.cli;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


// Flag Strings must start with "-", e.g., "-project"
// A Scribble extension should override getFlags
public class CLFlags
{
	public final Map<String, CLAux> flags;
	
	// Implicit (and unique)
	public static final String MAIN_MOD_FLAG = "__main";

	// Unique flags
	public static final String JUNIT_FLAG = "-junit";  // For internal use (JUnit test harness)
	public static final String IMPORT_PATH_FLAG = "-ip";
	public static final String API_OUTPUT_DIR_FLAG = "-d";
	public static final String VERBOSE_FLAG = "-V";
	public static final String STATECHAN_SUBTYPES_FLAG = "-subtypes";
	public static final String OLD_WF_FLAG = "-oldwf";
	public static final String NO_LIVENESS_FLAG = "-nolive";
	public static final String LTSCONVERT_MIN_FLAG = "-minlts";  // Currently only affects EFSM output (i.e. -fsm..) and API gen -- doesn't affect validation
	public static final String FAIR_FLAG = "-fair";
	public static final String NO_LOCAL_CHOICE_SUBJECT_CHECK_FLAG = "-nolocalchoicecheck";
	public static final String NO_ACCEPT_CORRELATION_CHECK_FLAG = "-nocorrelation";
	public static final String DOT_FLAG = "-dot";
	public static final String AUT_FLAG = "-aut";
	public static final String NO_VALIDATION_FLAG = "-novalid";
	public static final String INLINE_MAIN_MOD_FLAG = "-inline";
	public static final String SPIN_FLAG = "-spin";

	// Non-unique flags
	public static final String PROJECT_FLAG = "-project";
	public static final String EFSM_FLAG = "-fsm";
	public static final String VALIDATION_EFSM_FLAG = "-vfsm";
	public static final String UNFAIR_EFSM_FLAG = "-ufsm";
	public static final String UNFAIR_EFSM_PNG_FLAG = "-ufsmpng";
	public static final String EFSM_PNG_FLAG = "-fsmpng";
	public static final String VALIDATION_EFSM_PNG_FLAG = "-vfsmpng";
	public static final String SGRAPH_FLAG = "-model";
	public static final String UNFAIR_SGRAPH_FLAG = "-umodel";
	public static final String SGRAPH_PNG_FLAG = "-modelpng";
	public static final String UNFAIR_SGRAPH_PNG_FLAG = "-umodelpng";
	public static final String API_GEN_FLAG = "-api";
	public static final String SESSION_API_GEN_FLAG = "-sessapi";
	public static final String STATECHAN_API_GEN_FLAG = "-chanapi";
	public static final String EVENTDRIVEN_API_GEN_FLAG = "-cbapi";

	public CLFlags()
	{
		this.flags = Collections.unmodifiableMap(getFlags());
	}
		
	// Return a map of flag Strings to flag objects
	// A Scribble extension should override getFlags
	protected Map<String, CLAux> getFlags()
	{
		Map<String, CLAux> flags = new HashMap<>();

		// Unique; barrier irrelevant
		flags.put(IMPORT_PATH_FLAG, 
				new CLAux(IMPORT_PATH_FLAG, 1, true, false, "Missing path argument: "));
		flags.put(API_OUTPUT_DIR_FLAG, 
				new CLAux(API_OUTPUT_DIR_FLAG, 1, true, false,
						"Missing directory argument: "));
		flags.put(INLINE_MAIN_MOD_FLAG, 
				new CLAux(INLINE_MAIN_MOD_FLAG, 1, true, false, "Missing inline module: "));

		flags.put(JUNIT_FLAG, 
				new CLAux(JUNIT_FLAG, 0, true, false, "Duplicate flag: "));
		flags.put(VERBOSE_FLAG, 
				new CLAux(VERBOSE_FLAG, 0, true, false, "Duplicate flag: "));
		flags.put(STATECHAN_SUBTYPES_FLAG, 
				new CLAux(STATECHAN_SUBTYPES_FLAG, 0, true, false, "Duplicate flag: "));
		flags.put(OLD_WF_FLAG, 
				new CLAux(OLD_WF_FLAG, 0, true, false, "Duplicate flag: "));
		flags.put(NO_LIVENESS_FLAG, 
				new CLAux(NO_LIVENESS_FLAG, 0, true, false, "Duplicate flag: "));
		flags.put(LTSCONVERT_MIN_FLAG, 
				new CLAux(LTSCONVERT_MIN_FLAG, 0, true, false, "Duplicate flag: "));
		flags.put(FAIR_FLAG, 
				new CLAux(FAIR_FLAG, 0, true, false, "Duplicate flag: "));
		flags.put(NO_LOCAL_CHOICE_SUBJECT_CHECK_FLAG, 
				new CLAux(NO_LOCAL_CHOICE_SUBJECT_CHECK_FLAG, 0, true, false,
						"Duplicate flag: "));
		flags.put(NO_ACCEPT_CORRELATION_CHECK_FLAG, 
				new CLAux(NO_ACCEPT_CORRELATION_CHECK_FLAG, 0, true, false,
						"Duplicate flag: "));
		flags.put(NO_VALIDATION_FLAG, 
				new CLAux(NO_VALIDATION_FLAG, 0, true, false, "Duplicate flag: "));
		flags.put(SPIN_FLAG, 
				new CLAux(SPIN_FLAG, 0, true, false, "Duplicate flag: "));

		// These two are mutually exclusive
		flags.put(DOT_FLAG, 
				new CLAux(DOT_FLAG, 0, true, false, "Duplicate flag: ",
						CLFlags.AUT_FLAG));
		flags.put(AUT_FLAG, 
				new CLAux(AUT_FLAG, 0, true, false, "Duplicate flag: ",
						CLFlags.DOT_FLAG));

		// Non-unique, no barrier
		flags.put(PROJECT_FLAG, 
				new CLAux(PROJECT_FLAG, 2, false, false,
						"Missing protocol/role arguments: "));
		flags.put(EFSM_FLAG, 
				new CLAux(EFSM_FLAG, 2, false, false,
						"Missing protocol/role arguments: "));
		flags.put(VALIDATION_EFSM_FLAG, 
				new CLAux(VALIDATION_EFSM_FLAG, 2, false, false,
						"Missing protocol/role arguments: "));
		flags.put(UNFAIR_EFSM_FLAG, 
				new CLAux(UNFAIR_EFSM_FLAG, 2, false, false,
						"Missing protocol/role arguments: "));

		flags.put(EFSM_PNG_FLAG, 
				new CLAux(EFSM_PNG_FLAG, 3, false, false,
						"Missing protocol/role/file arguments: "));
		flags.put(VALIDATION_EFSM_PNG_FLAG, 
				new CLAux(VALIDATION_EFSM_PNG_FLAG, 3, false, false,
						"Missing protocol/role/file arguments: "));
		flags.put(UNFAIR_EFSM_PNG_FLAG, 
				new CLAux(UNFAIR_EFSM_PNG_FLAG, 3, false, false,
						"Missing protocol/role/file arguments: "));

		flags.put(SGRAPH_FLAG, 
				new CLAux(SGRAPH_FLAG, 2, false, false, "Missing protocol argument: "));
		flags.put(UNFAIR_SGRAPH_FLAG, 
				new CLAux(UNFAIR_SGRAPH_FLAG, 2, false, false,
						"Missing protocol argument: "));
		
		flags.put(SGRAPH_PNG_FLAG, 
				new CLAux(SGRAPH_PNG_FLAG, 1, false, false,
						"Missing protocol/file arguments: "));
		flags.put(UNFAIR_SGRAPH_PNG_FLAG, 
				new CLAux(UNFAIR_SGRAPH_PNG_FLAG, 1, false, false,
						"Missing protocol/file arguments: "));

		// // Non-unique, barrier
		flags.put(SESSION_API_GEN_FLAG, 
				new CLAux(SESSION_API_GEN_FLAG, 1, false, true,
						"Missing protocol argument: "));
		flags.put(API_GEN_FLAG, 
				new CLAux(API_GEN_FLAG, 2, false, true,
						"Missing protocol/role arguments: "));
		flags.put(STATECHAN_API_GEN_FLAG, 
				new CLAux(STATECHAN_API_GEN_FLAG, 2, false, true,
						"Missing protocol/role arguments: "));
		flags.put(EVENTDRIVEN_API_GEN_FLAG, 
				new CLAux(EVENTDRIVEN_API_GEN_FLAG, 2, false, true,
						"Missing protocol/role arguments: "));
		
		return Collections.unmodifiableMap(flags);
	}
}
