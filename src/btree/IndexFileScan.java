package btree;

import java.io.IOException;

import bufmgr.HashEntryNotFoundException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;


/**
 * Base class for a index file scan
 */
public abstract class IndexFileScan 
{
  /**
   * Get the next record.
   * @return the KeyDataEntry, which contains the key and data
 * @throws IOException 
 * @throws InvalidFrameNumberException 
 * @throws HashEntryNotFoundException 
 * @throws PageUnpinnedException 
 * @throws ReplacerException 
 * @throws Exception 
   */
  abstract public KeyDataEntry get_next() throws ReplacerException, PageUnpinnedException, HashEntryNotFoundException, InvalidFrameNumberException, IOException, Exception;

  /** 
   * Delete the current record.
 * @throws Exception 
   */
   abstract public void delete_current() throws Exception;

  /**
   * Returns the size of the key
   * @return the keysize
   */
  abstract public int keysize();
}
