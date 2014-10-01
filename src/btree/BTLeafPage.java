package btree;

import java.io.IOException;

import global.PageId;
import global.RID;
import global.SystemDefs;
import diskmgr.Page;
	
	/**
	 * BTLeafPage: This class is derived from BTSortedPage. It inserts records of the type key, 
	 * dataRid on the BTSortedPage. dataRid is the rid of the data record. The records are 
	 * sorted by the key. Further, leaf pages must be maintained in a doubly­linked list. 
	 * <key, list of rids of data records with search key>  alternative (3)
	 * don't use any private data members
	 * @author Ahmed & Ali clay
	 * 
	 */

public class BTLeafPage extends BTSortedPage{
	
	
	//these  constructors just call the parent constructor
	public BTLeafPage (int keyType)  throws ConstructPageException, Exception {
		super(keyType);
		setType(NodeType.LEAF);
	}
	
	public BTLeafPage (Page page, int keyType) throws ConstructPageException, IOException {
		super(page, keyType);
		
	}
	
	public BTLeafPage(PageId pageno, int keyType)  throws ConstructPageException, IOException {
		super(pageno, keyType);
		
	}
	
	
	
	/**
	 * insertRecord. READ THIS DESCRIPTION CAREFULLY. THERE ARE TWO RIDs WHICH MEAN TWO
	 * DIFFERENT THINGS. Inserts a key, rid value into the leaf node. 
	 * This is accomplished by a call to SortedPage::insertRecord() Parameters: 
	 * @param key the key value of the data record. Input parameter. 
	 * @param  dataRid the rid of the data record. This is stored on the leaf page 
	 *         along with the corresponding key value. Input parameter. 
	 * @return the rid of the inserted leaf record data entry, i.e., the pair. 
	 */
	 public RID insertRecord(KeyClass key,RID dataRid) throws Exception {
		 
		 KeyDataEntry entry = new  KeyDataEntry(key,dataRid);
		
		 try {
			 RID r = insertRecord(entry);
			 return r;
		 }catch(Exception e){System.out.println("insertion failed ");e.printStackTrace();}
		 return null;
	 }
	    
	    
	 
	 /**
	 * Iterators. One of the two functions: getFirst and getNext which provide an iterator interface to 
	 * the records on a BTLeafPage.
	 * @param key
	 * @param dataRid
	 * @return
	 */
	 public KeyDataEntry getFirst(RID rid) throws Exception {
		 
		 if(getSlotCnt() == 0)
			 return null;
		 
		 rid = firstRecord();
		 
		 KeyDataEntry first = BT.getEntryFromBytes(data, getSlotOffset(0), 
				 getSlotLength(0), keyType, NodeType.LEAF);
		 
		 return first;
	 }
	 
	 
	 public KeyDataEntry getNext(RID rid) throws Exception {
		 
		//next entry
		 rid.slotNo++;
		 if(getSlotCnt() <= rid.slotNo || rid.slotNo < 0) {// no more records
			 return null;
		 }
		 
		 KeyDataEntry entry  = BT.getEntryFromBytes(data, getSlotOffset(rid.slotNo),
				 getSlotLength(rid.slotNo), keyType, NodeType.LEAF);
		 
		 return entry;
	 }
	 
	 
	 
	 
	 /**
	 * getCurrent returns the current record in the iteration; 
	 * it is like getNext except it does not advance the iterator. 
	 * @param key
	 * @param dataRid
	 * @return
	 */
	 public KeyDataEntry getCurrent(RID rid) throws Exception {
		 
		 if(rid.slotNo >= getSlotCnt() || rid.slotNo < 0)
			 return null;
		 
		 return BT.getEntryFromBytes(data, getSlotOffset(rid.slotNo), 
				 getSlotLength(rid.slotNo), keyType, NodeType.LEAF);
	 }

	 
	/**
	 * delete a data entry in the leaf page. 
	 * @param dEntry
	 * @return
	 */
	 public boolean delEntry(KeyDataEntry dEntry) {
		 
		
		 try {
			 for (int i = 0 ; i < getSlotCnt(); i++) {
				 KeyDataEntry entry =  BT.getEntryFromBytes(data, getSlotOffset(i), 
						 getSlotLength(i), keyType, NodeType.LEAF);
				 
				 if(dEntry.equals(entry)) {
					 return deleteSortedRecord(new RID(new PageId(curPage.pid), i));
				 }
			 }
		 }catch(Exception e) {
			 return false;
		 }
		 
		 return false;
	 }
	 
	 
	 public void unpin(boolean dirty) throws Exception{
		 SystemDefs.JavabaseBM.unpinPage(getCurPage(), dirty);
	 }
	 
	
}
