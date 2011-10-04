// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//
package org.pathvisio.go;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pathvisio.obo.OboHandler;
import org.pathvisio.obo.OboReader;

/**
 * The class GoReader contains two methods to read the data of the GO terms. 
 */
public class GoReader implements OboHandler
{
	private final Set<GoTerm> terms;
	private final Map<String, GoTerm> idGoTerm;
	private final Map<GoTerm, Set<String>> goTermParents = new HashMap<GoTerm,Set<String>>();
	
	public GoTerm findTerm (String id)
	{
		return idGoTerm.get(id);
	}
	
	public Set<GoTerm> getTerms()
	{
		return terms;
	}
	
	public GoReader (File obo)
	{		
		terms = new HashSet<GoTerm>();
		idGoTerm = new HashMap<String,GoTerm>();

		FileReader fr = null;
		try {
			fr = new FileReader(obo);
			BufferedReader br = new BufferedReader(fr);
			
			OboReader.parse(br, this);
		}		
		catch(Exception e) 
		{
			System.out.println("Exception: " + e);
			e.printStackTrace();
		}
		finally
		{
			try	{ fr.close(); }	catch (IOException e) {	/* ignore */ }
		}
		
		/**
		 * In a for-loop, first for all GoTerms the parents are returned.
		 * If these parents exist, the second for-loop walks through all these parents. The GoTerm
		 * of this parent is added (as a value) to a map with current GoTerm (as a key).
		 * Also, in a map the The GoTerm of this parent is added (as a key) to a map with current 
		 * GoTerm (as a value).
		 * So two maps are created: 
		 * One with the children as a key and the parents as a value.
		 * And one with the parents as a key and the children as a value.   
		 */
		// now loop through all GoTerms
		for (GoTerm thisTerm : terms)
		{
			// get the parents (strings) of this goTerm
			Set<String> parents = goTermParents.get(thisTerm);
			System.out.println (thisTerm + "");
			if (!parents.isEmpty())
			{
				// loop through all these parent strings
				for(String parent : parents)
				{
					System.out.println ("P: " + parent + "");
					// get the goTerm beloning to the parent string (the parent string
					// contains the id of the parent, the second map contains the id's
					// with the goterms belonging to this id)
					GoTerm ouder = idGoTerm.get(parent);
					// add the found parent GoTerm as a parent for the read GoTerm
					thisTerm.addParent(ouder);
					// the read GoTerm is a child of it's parent; so add a child to
					// the parent GoTerm
					ouder.addChild(thisTerm);
				}
			}
		}
			
		// show a message that everything is read; and return the terms
		System.out.println("DB read");
	}
	
	/**
	 * In the method 'getRoots' for a set of GoTerms the roots are returned in a set.
	 */
	
	public List<GoTerm> getRoots()
	{
		// create a list for the roots
		List<GoTerm> roots = new ArrayList<GoTerm>();
		// walk through the terms to find the roots
		for (GoTerm term : terms)
		{
			// if a term has no parents, it's a root
			if(!term.hasParents()){
				// add the term as root
				roots.add(term);
			}				
		}
		// return the root list
		return roots;
	}
	
	String id;
	String name;
	String namespace;
	Set<String> isa;
	boolean obsolete;
	String block;
	
	@Override
	public void startBlock(String type)
	{
		id = null;
		name = null;
		namespace = null;
		isa = new HashSet<String>();
		obsolete = false;
		block = type;
	}

	@Override
	public void property(String prop, String value)
	{
		if (!block.equals("Term")) return;
		if (prop.equals("id"))
		{
			id = value;
		}
		else if (prop.equals ("name"))
		{
			name = value;
		}
		else if (prop.equals ("namespace"))
		{
			namespace = value;
		}
		else if (prop.equals ("is_obsolete"))
		{
			obsolete = value.equals("true");
		}
		else if (prop.equals("is_a"))
		{
			// we only need the first ten characters, the GO id. 
			isa.add (value.substring(0,10));
		}		
	}

	@Override
	public void endBlock()
	{
		if (!block.equals("Term")) return;
		if (!obsolete)
		{
			// if the term isn't obsolete, create the GoTerm
			GoTerm newGoTerm = new GoTerm(id,name,namespace);
			// and add this term to the 'terms' set
			terms.add(newGoTerm);
			// and at this term and it's list of parents to the first map
			goTermParents.put(newGoTerm, isa);
			// and add this term's id and the term itself to the second map
			idGoTerm.put(id, newGoTerm);
		}
	}
}
