package org.pathvisio.obo;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse the obo file format. Readers must implement the OboHandler callback handler, 
 * and then call parse()
 */
public final class OboReader
{
	private OboReader() {} // static utility class, not meant for instantiation.

	private static Pattern pat = Pattern.compile ("([^:]+):\\s*(.*)");

	public static void parse (BufferedReader br, OboHandler handler) throws IOException
	{
		String line;
		String blockType = "Main";
		
		handler.startBlock(blockType);

		// Read line-by-line until the end is reached
		while((line = br.readLine()) != null){
			// If the line starts with a term, process it
			if(line.startsWith("[Term]"))
			{
				blockType = "Term";
				handler.startBlock(blockType);
			}
			else if (line.startsWith("[Typedef]"))
			{
				blockType = "Typedef";
				handler.startBlock(blockType);
			}	
			else if (line.equals (""))
			{
				handler.endBlock();
			}
			else
			{
				Matcher mat = pat.matcher(line);
				if (!mat.matches())
				{
					throw new IllegalStateException("Cannot parse " + line);
				}				
				handler.property(mat.group(1), mat.group(2));
			}
		}
	}	
}
