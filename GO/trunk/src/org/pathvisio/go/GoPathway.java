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

import java.io.File;
import java.util.Set;

import javax.swing.JOptionPane;

import org.bridgedb.bio.BioDataSource;
import org.bridgedb.AttributeMapper;
import org.bridgedb.BridgeDb;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.rdb.IDMapperRdb;
import org.bridgedb.rdb.SimpleGdbFactory;
import org.bridgedb.rdb.construct.DataDerby;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.GroupStyle;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.core.util.Utils;
import org.pathvisio.gui.ProgressDialog;

/**
 * Run GoPathway with the following command line arguments (5 or more):
 * 
 * Argument 1: file name of gene ontology obo definition file. The latest one can be downloaded from 
 * 	http://www.geneontology.org/GO.downloads.ontology.shtml
 * 
 * Argument 2: a mapping of ensembl to gene ontology, which can be downloaded from 
 * Ensembl BioMart (http://www.ensembl.org/biomart/martview/)
 * This should be a tab-delimited text file with 
 * exactly one header row, with the following columns:
 * 	Column 1: Ensembl gene ID
 *  Column 2: GO ID
 *  Any more columns are ignored if present.
 *  (NB: Make sure you don't have a column with Ensembl transcript ID's)
 *  
 * Argument 3: file name of a PathVisio pgdb database for the correct species.
 * 
 * Argument 4: output directory, This should be an existing directory
 * 
 * Argument 5 and on: GO ID to make pathways of
 * 
 * For example
 * 
		/home/martijn/db/go/gene_ontology.obo
		/home/martijn/Desktop/rest/hs_e50_goid_mart.txt
		"/home/martijn/PathVisio-Data/gene databases/Hs_Derby_20080102_ens_workaround.pgdb"
		/home/martijn/Desktop
		GO:0045086
		GO:0051260
		GO:0045404
		GO:0030154
 */
public class GoPathway 
{
	private void run (String[] args) throws IDMapperException
	{
		File obo = new File (args[0]);
		File mart = new File (args[1]);
		String gdbname = args[2]; 
		File destDir = new File (args[3]);

		if (!obo.exists() || !mart.exists() || !destDir.isDirectory())
		{
			throw new IllegalArgumentException();
		}

		try
		{
			Class.forName ("org.bridgedb.rdb.IDMapperRdb");
		}
		catch (ClassNotFoundException e1)
		{
			throw new IDMapperException(e1);
		}
		IDMapper gdb = BridgeDb.connect ("idmapper-pgdb:" + gdbname); 
		for (int i = 4; i < args.length; ++i)
		{
			String goid = args[i];
			
			
			GoReader reader = new GoReader(obo);
			
			Logger.log.info ("Go terms read: " + reader.getTerms().size());
			
			GoTerm term = reader.findTerm(goid);
			Logger.log.info (goid);
			if (term == null) throw new NullPointerException();
			
			Pathway p = makeGoPathway (reader, term, gdb, (AttributeMapper)gdb, null);
			p.getMappInfo().setAuthor("Martijn van Iersel");
			p.getMappInfo().setMapInfoDataSource("Gene Ontology");
			p.getMappInfo().setEmail("martijn.vaniersel@bigcat.unimaas.nl");
			
			String name = "Hs_GO_" + term.getName();
			if (name.length() >= 50) name = name.substring (0, 50);
			
			p.getMappInfo().setMapInfoName(name);
			
			try
			{
				p.writeToXml(new File (destDir, "Hs_GO_" + term.getName() + ".gpml"), true);
			}
			catch (ConverterException e)
			{
				Logger.log.error ("",e);
			}
		}
	}
	
	Pathway makeGoPathway (GoReader reader, GoTerm base, IDMapper gdb, AttributeMapper adb, ProgressKeeper pk) throws IDMapperException
	{
		Pathway result = new Pathway();

		double top = 60;
		double left = 60;

		addIds (result,  base, left, top, gdb, adb, null, pk);
		
		return result;
	}
	
	static final int MAXCOLNUM = 10;
	static final double DATANODEWIDTH = 100;
	static final double DATANODEHEIGHT = 20;
	static final double LABELWIDTH = 400;
	static final double LABELHEIGHT = 20;
	static final double MARGIN = 7;
	static final double COLWIDTH = MAXCOLNUM * (DATANODEWIDTH + MARGIN);
	static final double INDENT = 30;

	double addIds (Pathway p, GoTerm term, double left, double top, IDMapper gdb, AttributeMapper adb, String parentGroup, ProgressKeeper pk) throws IDMapperException
	{
		pk.setProgress(0);
		pk.setTaskName("Go term: " + term.getName());

		String ds = PreferenceManager.getCurrent().get (GoPlugin.GoPreference.GO_PLUGIN_TARGET_DATASOURCE);
		//TODO: make configurable with drop-down box.
		DataSource dest = DataSource.getByFullName(ds);
		
		// ignore if there are no genes corresponding to this tree.
		if (GoMap.getRefsRecursive(term, gdb, dest).size() == 0) return top; 
		
		Set<Xref> refs = GoMap.getRefs(term, gdb, dest);

		double xco = 0;
		double yco = 0;

		PathwayElement group = PathwayElement.createPathwayElement(ObjectType.GROUP);
		group.setGroupStyle(GroupStyle.COMPLEX);
		group.setTextLabel(term.getId());

		if (parentGroup != null)
		{
			group.setGroupRef(parentGroup);
		}
		
		p.add (group);
		String groupRef = group.createGroupId();
		
		PathwayElement label = PathwayElement.createPathwayElement(ObjectType.LABEL);
		
		label.setMCenterX(left + LABELWIDTH / 2);
		label.setMCenterY(top + LABELHEIGHT / 2);
		label.setMWidth(LABELWIDTH);
		label.setMHeight(LABELHEIGHT);		
		label.setTextLabel(term.getId() + " " + term.getName());
		label.setGroupRef(groupRef);
		
		top += LABELHEIGHT + MARGIN;
		
		p.add(label);
		
		int i = 0;
		
		for (Xref ref : refs)
		{
			PathwayElement pelt = PathwayElement.createPathwayElement(ObjectType.DATANODE);
			
			String symbol = null;
			try
			{
				symbol = Utils.oneOf(adb.getAttributes(ref, "Symbol")); 
			}
			catch (IDMapperException ex)
			{
				Logger.log.warn ("Failed lookup of gene symbol", ex);
			}
			if (symbol == null) symbol = ref.getId();
			
			pelt.setMCenterX(left + xco + DATANODEWIDTH / 2);
			pelt.setMCenterY(top + yco + DATANODEHEIGHT / 2);
			pelt.setMWidth(DATANODEWIDTH);
			pelt.setMHeight(DATANODEHEIGHT);
			
			pelt.setDataSource(ref.getDataSource());
			pelt.setGeneID(ref.getId());
			pelt.setDataNodeType(DataNodeType.GENEPRODUCT);
			pelt.setTextLabel(symbol);
			pelt.setGroupRef (groupRef);
			
			p.add(pelt);
			
			xco += DATANODEWIDTH + MARGIN;
			
			if (xco >= COLWIDTH)
			{
				xco = 0;
				yco += DATANODEHEIGHT;
			}
			
			pk.setProgress(i++ * 60 / refs.size());
		}
		
		double bottom = top + yco;
		
		if (refs.size() > 0)
		{
			bottom += DATANODEHEIGHT * 2;
		}
		
		i = 0;
		for (GoTerm child : term.getChildren())
		{
			bottom = addIds (p, child, left + INDENT, bottom, gdb, adb, groupRef, pk);
			pk.setProgress(60 + (i * 40 / term.getChildren().size()));
		}
		
		return bottom + DATANODEHEIGHT;
	}
	
	
	public static void main(String [] args) throws IDMapperException
	{
		GoPathway pathway = new GoPathway();
		pathway.run (args);
		Logger.log.info ("DONE");
	}
}
