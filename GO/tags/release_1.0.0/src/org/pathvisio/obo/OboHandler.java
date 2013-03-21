package org.pathvisio.obo;

/** 
 * Callback handler for parsing Obo files.
 */
public interface OboHandler
{
	/**
	 * Called at the beginning of each block in the Obo file. There are three types of blocks:
	 * Term blocks, Typedef blocks and the first block that doesn't have a header, that 
	 * we'll call the Main block.
	 * @param block: one of "Term", "Typedef" or "Main"
	 */
	public void startBlock(String block);
	
	/**
	 * Called for each property in a block.
	 * @param name the property, e.g. "id" or "is_a"
	 * @param value the value of the property
	 */
	public void property(String name, String value);
	
	/**
	 * Called when the block ends
	 */
	public void endBlock();
}
