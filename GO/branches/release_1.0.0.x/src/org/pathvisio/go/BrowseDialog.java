package org.pathvisio.go;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;

import org.pathvisio.gui.dialogs.OkCancelDialog;

public class BrowseDialog extends OkCancelDialog
{
	private TreeModel model;
	private JTree tree;
	
	public BrowseDialog(Frame frame, GoReader reader)
	{
		super(frame, "Browse the Gene Ontology", frame, false);
		model = new GoTreeModel(reader.getRoots());		
		setDialogComponent(createDialogPane());
		pack();
	}
	
	protected Component createDialogPane()
	{
	    JPanel panel = new JPanel();
	    panel.setLayout(new BorderLayout());
	    tree = new JTree(model);
	    panel.add(new JScrollPane(tree), BorderLayout.CENTER);
	    
	    //TODO add selection listener to tree
	    
	    return panel;
	}

}
