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

import java.util.HashSet;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.debug.Logger;

/**
 * Helps with mappings from GO to genes and vice versa
 */
public abstract class GoMap 
{

	/**
	 * Helper function to recursively extract genes from a mapper.
	 */
	public static Set<Xref> getRefs(GoTerm term, IDMapper mapper, DataSource dest) throws IDMapperException
	{
		 return mapper.mapID(new Xref (term.getId(), BioDataSource.GENE_ONTOLOGY), dest);	
	}
	
	/**
	 * Helper function to recursively extract genes from a mapper.
	 */
	public static Set<Xref> getRefsRecursive(GoTerm term, IDMapper mapper, DataSource dest) throws IDMapperException
	{
		Set<Xref> result = new HashSet<Xref>();
		for (GoTerm child : term.getChildren())
		{
			result.addAll(getRefsRecursive(child, mapper, dest));
		}
		result.addAll (getRefs (term, mapper, dest));
		Logger.log.info (term.getName() + " has " + result.size() + " ids");
		
		return result;
	}

}
