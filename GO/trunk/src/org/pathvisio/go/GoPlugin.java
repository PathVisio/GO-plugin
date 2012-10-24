package org.pathvisio.go;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.bridgedb.IDMapperStack;
import org.bridgedb.bio.BioDataSource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.preferences.Preference;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.pathvisio.desktop.util.BrowseButtonActionListener;
import org.pathvisio.gui.ProgressDialog;
import org.pathvisio.gui.SwingEngine;
import org.pathvisio.gui.dialogs.OkCancelDialog;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * GUI interface for GoPathway script.
 * TODO: Work in Progress
 */
public final class GoPlugin implements Plugin, BundleActivator
{
	private PvDesktop desktop;
	private GoAction goAction;
	private GoBrowseAction browseAction;
	
	public void init(PvDesktop desktop) 
	{
		this.desktop = desktop;
		goAction = new GoAction();
		browseAction = new GoBrowseAction();
		desktop.registerMenuAction("Data", goAction);
		desktop.registerMenuAction("Data", browseAction);
	}

	public void done() {
		desktop.unregisterMenuAction("Data", goAction);
		desktop.unregisterMenuAction("Data", browseAction);
	}

	private class GoAction extends AbstractAction
	{
		GoAction ()
		{
			putValue (NAME, "Create GO Pathway");	
		}

		public void actionPerformed(ActionEvent arg0) 
		{
			GoPluginFrame frame = new GoPluginFrame();
			frame.setVisible(true);
		}
	}

	private class GoBrowseAction extends AbstractAction
	{
		GoBrowseAction ()
		{
			putValue (NAME, "Browse GO Ontology");	
		}

		public void actionPerformed(ActionEvent arg0) 
		{
			//TODO: deal with situation when preference is not set or not correct
			GoReader reader = new GoReader(PreferenceManager.getCurrent().getFile(GoPreference.GO_PLUGIN_OBO_FILE));
			BrowseDialog frame = new BrowseDialog(desktop.getFrame(), reader);
			frame.setVisible(true);
		}
	}

	public static enum GoPreference implements Preference
	{
		GO_PLUGIN_GO_ID("GO:0006096"), // go id for glycolysis, useful for testing
		GO_PLUGIN_OBO_FILE(System.getProperty("user.home") + File.separator + "gene_ontology.obo"), // gene_ontology.obo
		GO_PLUGIN_TARGET_DATASOURCE(BioDataSource.ENSEMBL_HUMAN.getFullName());
		;

		private final String defaultVal;
		
		GoPreference (String _defaultVal) { defaultVal = _defaultVal; }
		
		@Override
		public String getDefault() { return defaultVal; }
	}

	private class GoPluginFrame extends OkCancelDialog
	{		
		GoPluginFrame ()
		{
			super (desktop.getSwingEngine().getFrame(), "Create GO Dialog", 
					desktop.getSwingEngine().getFrame(), true);
			setDialogComponent(createDialogPane());
			pack();
		}
		
		JTextField txtGoId;
		JTextField txtOboFile;
		JButton browseObo;
				
		protected Component createDialogPane() 
		{
		    FormLayout layout = new FormLayout (
		    		"4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu",
		    		"4dlu, pref, 4dlu, pref, 4dlu");
		    JPanel panel = new JPanel(layout);
		    CellConstraints cc = new CellConstraints();
		    
		    txtGoId = new JTextField (20);
		    txtGoId.setText (PreferenceManager.getCurrent().get(GoPreference.GO_PLUGIN_GO_ID));
		    txtOboFile = new JTextField (40);
		    txtOboFile.setText (PreferenceManager.getCurrent().get(GoPreference.GO_PLUGIN_OBO_FILE));
		    
		    browseObo = new JButton ("Browse");
		    browseObo.addActionListener(new BrowseButtonActionListener (txtOboFile, desktop.getFrame(), JFileChooser.FILES_ONLY));
		    
		    panel.add(new JLabel("GO ID"), cc.xy(2,2));
		    panel.add(txtGoId, cc.xy(4, 2));

		    panel.add(new JLabel("Obo File"), cc.xy(2,4));
		    panel.add(txtOboFile, cc.xy (4,4));
		    panel.add(browseObo, cc.xy (6,4));
		    
			return panel;
		}

		protected void okPressed() 
		{	
			PreferenceManager.getCurrent().set (GoPreference.GO_PLUGIN_GO_ID, txtGoId.getText());
			PreferenceManager.getCurrent().set (GoPreference.GO_PLUGIN_OBO_FILE, txtOboFile.getText());
			final String id = txtGoId.getText();
			final File oboF = new File(txtOboFile.getText());
			if (!oboF.exists())
			{
				JOptionPane.showMessageDialog(this, "Obo File not found", 
						"Obo File not found", JOptionPane.ERROR_MESSAGE);
				return;
			}
			final GoReader reader = new GoReader(oboF);
			final GoTerm base = reader.findTerm(id);
			if (base == null) 
			{
				JOptionPane.showMessageDialog(this, "Go Term not found", 
						"Go Term not found", JOptionPane.ERROR_MESSAGE);
				return;
			}

			final ProgressKeeper pk = new ProgressKeeper(100);
			final ProgressDialog d = new ProgressDialog(desktop.getFrame(), "", pk, false, true);

			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
			{
				private Pathway result;
				
				@Override
				public Void doInBackground()
				{			
					try
					{
						GoPathway goPwy = new GoPathway();
		
						IDMapperStack stack = desktop.getSwingEngine().getGdbManager().getCurrentGdb();
						result = goPwy.makeGoPathway(reader, base, 
								stack, stack, pk);
					}
					catch (Exception ex)
					{
						Logger.log.error ("Error during GO Pathway creation", ex);
						//TODO: pop up dialog
					}
					finally
					{
						pk.finished();
					}
					
					return null;
				}
				
				@Override
				public void done()
				{
					SwingEngine se = desktop.getSwingEngine(); 
					se.getEngine().setWrapper(se.createWrapper());
					se.getEngine().createVPathway(result);
					GoPluginFrame.super.okPressed();
				}
			};

			if (desktop.getSwingEngine().canDiscardPathway())
			{
				worker.execute();
				d.setVisible(true);
			}
			
		}
		
	}

	@Override
	public void start(BundleContext context) throws Exception {

		context.registerService(Plugin.class.getName(), this, null);
		
	}

	@Override
	public void stop(BundleContext context) throws Exception 
	{
		done();
	}
}
