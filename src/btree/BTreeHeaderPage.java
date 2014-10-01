package btree;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;

import diskmgr.Page;

import global.PageId;
import global.RID;
import global.SystemDefs;
import heap.HFPage;


public class BTreeHeaderPage extends HFPage {
	

	/**
	 * this class has no documentation ,
	 * what are we going to do ? 
	 * take from every man a tribe
	 * @return
	 * @throws IOException 
	 */
	

///////////////////////////////////////////////////////////////////////////////////////
	
	
	 
	  /**
	   * pin the header page with the give pageId
	   * @param id
	   * @throws Exception
	   */
	  public BTreeHeaderPage(PageId id) throws Exception { 
		  super();
	      SystemDefs.JavabaseBM.pinPage(id, this, false); 
	     
	  }
	  
	  /**
	   * just call the super constructor
	   * @param page
	   */
	  public BTreeHeaderPage(Page page) {
		  super(page);
	  }  
	  
	  
	  /**
	   * create a new BTHeaderPage and pin it
	   * @throws Exception
	   */
	  public BTreeHeaderPage() throws Exception {
		  	super();
			Page p = new Page();
			PageId pageId=SystemDefs.JavabaseBM.newPage(p,1);
			this.init(pageId, p);
			setType(NodeType.BTHEAD);
			
			
			//set the key type
			BTIndexPage indexPage = new BTIndexPage(this,global.AttrType.attrInteger);
			KeyDataEntry entry = new KeyDataEntry(0, new PageId(-1));
			indexPage.insertRecord(entry);
			
			//set the key size
			entry = new KeyDataEntry(1, new PageId(-1));
			indexPage.insertRecord(entry);
			
			
	  }  
	
	
	
	
	
	/**
	 * use the nextPage pointer as a pointer to the root
	 * @return
	 * @throws IOException
	 */
	public PageId get_rootId() throws IOException{
		return getNextPage();
	}
	
	
	public void set_rootId(PageId rootId) throws IOException{
		setNextPage(rootId);
	}
	
///////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * key type will be inserted as a record
	 * in the HFPage , it will be the only record
	 * @return
	 */
	
	public short get_keyType() {
		
		try {
			
			BTIndexPage indexPage = new BTIndexPage(this,global.AttrType.attrInteger);
			KeyDataEntry entry = indexPage.getFirst(new RID());
			return (short)((IndexData)entry.data).getData().pid;
			
			
		} catch (Exception e) {
		
		}
		
		return -1;
	}
	
	
	
	
	public void set_keyType(int classType) throws Exception{
			

		try {
			
			BTIndexPage indexPage = new BTIndexPage(this,global.AttrType.attrInteger);
			RID id = new RID();
			KeyDataEntry entry = indexPage.getFirst(id);
			((IndexData)entry.data).setData(new PageId(classType));
			indexPage.deleteRecord(id);
			indexPage.insertRecord(entry);
			
			
			
		} catch (Exception e) {
			System.out.println("set_keyType Failed !");
			e.printStackTrace();
		}
		
	}
	
	
	
	public void set_maxKeySize(int keySize ){

		try {
			
			BTIndexPage indexPage = new BTIndexPage(this,global.AttrType.attrInteger);
			RID id = new RID();
			KeyDataEntry entry = indexPage.getFirst(id);
			entry = indexPage.getNext(id);
			((IndexData)entry.data).setData(new PageId(keySize));
			indexPage.deleteRecord(id);
			indexPage.insertRecord(entry);
			
		} catch (Exception e) {
			System.out.println("setMaxKeySize Failed !");
			e.printStackTrace();
		}
		
	}
	
	public int get_maxKeySize() {
		try {
			
			BTIndexPage indexPage = new BTIndexPage(this,global.AttrType.attrInteger);
			RID id = new RID();
			KeyDataEntry entry = indexPage.getFirst(id);
			entry = indexPage.getNext(id);
			return (short)((IndexData)entry.data).getData().pid;
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return -1;
	} 
	

	
	
}
