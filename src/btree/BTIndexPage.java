package btree;

import java.io.IOException;

import global.PageId;
import global.RID;
import global.SystemDefs;
import diskmgr.Page;
	
	/**
	 * BTIndexPage: This class is derived from BTSortedPage. It inserts records of the type 
	 * key, pageNo on the BTSortedPage. The records are sorted by the key. 
	 * don't use any private data members
	 * the prevPage Pointer will be used to store P0.
	 * @author Ahmed & Ali
	 *
	 */

public class BTIndexPage extends BTSortedPage {

	
	//these constructors just call the super constructors
	//of BTSortedPage which don't set type of the page
	public BTIndexPage(int keyType) throws Exception {
		super(keyType);
		// HFSet the type of page
	    // the super constructor doesn't set the type of the page
	    // it sets some private variable nothing more
	    // it's important in order to know whether you reached a leaf page or not.
	    setType(NodeType.INDEX);
	}
	
	public BTIndexPage(Page page, int keyType)  throws Exception {
		super(page, keyType);
	    
	}
	
	public BTIndexPage(PageId pageno, int keyType)  throws Exception {
		      super(pageno, keyType);
		      
    }

	
	
	
	/**
	 * It inserts a value into the index page
	 * @param key
	 * @param pageNo
	 * @return
	 * @throws InsertRecException 
	 */
	public RID insertKey(KeyClass key,PageId pageNo) throws InsertRecException {
		
		KeyDataEntry record = new KeyDataEntry(key, pageNo);
		// insertRecord in the parent calls HFPage which returns null
		// if insertion failed , so everything is handled.
		return insertRecord(record);
	}
	
	
	/**
	 * what is the purpose of this function ?
	 * well it will be used to search for the pointer of next page
	 * using a search key.
	 * 1- check if the next page is the most left one
	 * 2- search the rest of page for the the key
	 * @param key : to search with
	 * @return PageId of next page to be accessed
	 */
	 public PageId getPageNoByKey(KeyClass key) throws Exception{
		 
		
		  
		 KeyDataEntry searchEntry ;
		 for (int i = getSlotCnt()-1 ; i >= 0 ; i--) {
			 
			 searchEntry = BT.getEntryFromBytes(data, getSlotOffset(i), getSlotLength(i),
					 keyType, NodeType.INDEX);
			 
			 if(BT.keyCompare(searchEntry.key, key) <= 0) {
				 return ((IndexData)(searchEntry.data)).getData();
			 }
		 }
		 
		 //smaller than the smallest key 
		 return getPrevPage();
	 }
	 
	 
	 
	 
	 /**
	  * Left Link You will recall that the index pages have a left-most pointer
	  * that is followed whenever the search key value is less than the least 
	  * key value in the index node. The previous page pointer is used to implement 
	  * the left link. 
	  * @return
	  */
	 public PageId getLeftLink() throws Exception{
		 return getPrevPage();
	 }

	 
	 /**
	  * You will recall that the index pages have a left-most pointer that is 
	  * followed whenever the search key value is less than the least key value 
	  * in the index node. The previous page pointer is used to implement the left link.
	  * The function sets the left link. 
	  * @param left
	  */
	 public void setLeftLink(PageId left) throws Exception{
		 setPrevPage(left);
	 }
	 
	 
	 
	 
	 
	 
////////////////////////////////////////////////////////////////////////////////////////////////
	 
	 //iterating
	 public KeyDataEntry getFirst(RID rid) throws Exception {
		 
		 //check if there is no keyDatatEntrys
		 if(getSlotCnt() == 0) {
		    	return null;
		 } 
		
		KeyDataEntry entry = BT.getEntryFromBytes(data, getSlotOffset(0),getSlotLength(0),
			 keyType, NodeType.INDEX);
		
		//set the first rid
		rid = firstRecord();
		
		return entry;
	 }
	 
	 
	 
	 public KeyDataEntry getNext(RID rid) throws Exception {
		 
		 //next entry
		 rid.slotNo++;
		 if(getSlotCnt() <= rid.slotNo || rid.slotNo < 0) {// no more records
			 return null;
		 }
		 
		 KeyDataEntry entry  = BT.getEntryFromBytes(data, getSlotOffset(rid.slotNo),
				 getSlotLength(rid.slotNo), keyType, NodeType.INDEX);
		 
		 return entry;
	 }
	 
	 
	 public void unpin(boolean dirty) throws Exception{
		 SystemDefs.JavabaseBM.unpinPage(getCurPage(), dirty);
	 }
	

}// end of class
