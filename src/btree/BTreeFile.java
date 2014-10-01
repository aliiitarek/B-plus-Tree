package btree;

import diskmgr.Page;
import global.PageId;
import global.RID;
import global.SystemDefs;
import heap.HFPage;

public class BTreeFile extends IndexFile {

	private String fileName;
	private BTreeHeaderPage headerPage;
	private int maxSize;
	private boolean closed;

	/**
	 * if index file exists, open it; else create it. bin the header page.
	 * 
	 * @param filename
	 *            - file name. Input parameter.
	 * @param keytype
	 *            - the type of key. Input parameter.
	 * @param keysize
	 *            - the maximum size of a key. Input parameter.
	 * @param delete_fashion
	 *            - full delete or naive delete. Input parameter. It is either
	 *            DeleteFashion.NAIVE_DELETE or DeleteFashion.FULL_DELETE. set
	 *            it to zero in this assignment.
	 */
	public BTreeFile(String filename, int keytype, int keysize,
			int delete_fashion) throws Exception {

		// check if the file already exists
		if (SystemDefs.JavabaseDB.get_file_entry(filename) != null) {
			throw new IllegalArgumentException("file already exists !");
		}

		this.fileName = filename;

		// header page setup
		headerPage = new BTreeHeaderPage();
		SystemDefs.JavabaseDB.add_file_entry(filename, headerPage.getCurPage());
		headerPage.set_keyType((short) keytype);
		headerPage.set_maxKeySize(maxSize);
		headerPage.set_rootId(new PageId(global.GlobalConst.INVALID_PAGE));

		closed = false;

	}

	/**
	 * BTreeFile class an index file with given filename should already exist;
	 * this opens it. pin the header page.
	 * 
	 * @param filename
	 *            - the B+ tree file name. Input parameter.
	 */
	public BTreeFile(String fileName) throws Exception {

		this.fileName = fileName;
		PageId headerID = SystemDefs.JavabaseDB.get_file_entry(fileName);
		headerPage = new BTreeHeaderPage(headerID);
		closed = false;
	}

	/**
	 * Insert entry into the index file.
	 * 
	 * @param data
	 *            the key for the entry
	 * @param rid
	 *            the rid of the tuple with the key
	 */
	@Override
	public void insert(KeyClass data, RID rid) {

		try {

			// if (BT.getKeyLength(data) > headerPage.get_maxKeySize())
			// throw new IllegalArgumentException("key size is to big");

			RID dummpy = new RID();
			dummpy.copyRid(rid);
			KeyDataEntry entry = new KeyDataEntry(data, dummpy);

			PageId rootId = headerPage.get_rootId();

			if (rootId.pid == global.GlobalConst.INVALID_PAGE) {// check if
																// there is no
																// root
				// create new root which will be leafPage and set the header
				// page to point to it
				BTLeafPage root = new BTLeafPage(headerPage.get_keyType());
				headerPage.set_rootId(root.getCurPage());
				unpin(root);
			}

			int x = SystemDefs.JavabaseBM.getNumUnpinnedBuffers();

			BTSortedPage root = pinPage(headerPage.get_rootId());

			entry = insertRecursive(root, entry);
			if (entry != null) {
				throw new Exception("insert recursive failed the key : "
						+ (entry.key + "," + entry.data) + " before : " + x
						+ " after : "
						+ SystemDefs.JavabaseBM.getNumUnpinnedBuffers()
						+ " is returned ");
			}

		} catch (Exception e) {
			System.err.print("insertion failed "
					+ SystemDefs.JavabaseBM.getNumUnpinnedBuffers() + "\n");
			e.printStackTrace();
			System.err.print("end of failed insertion\n");
		}

	}// end of insert

	private KeyDataEntry insertRecursive(BTSortedPage nodePointer,
			KeyDataEntry entry) throws Exception {

		// if nodePinter is index type
		if (nodePointer.getType() == NodeType.INDEX) {
			return insertInIndexPage((BTIndexPage) nodePointer, entry);
		}

		else {// nodePointer is a leafPage
			return insertInLeafPage((BTLeafPage) nodePointer, entry);
		}

	}

	private KeyDataEntry insertInIndexPage(BTIndexPage indexPage,
			KeyDataEntry entry) throws Exception {

		// get sub tree
		PageId subTree = indexPage.getPageNoByKey(entry.key);

		// insert into the sub tree
		BTSortedPage child = pinPage(subTree);
		KeyDataEntry newChildEntry = insertRecursive(child, entry);

		// no split propagated to me
		if (newChildEntry == null) {
			unpin(indexPage);
			return null;
		}

		// try to insert propagated split in this index page
		if (pageInsertion(indexPage, newChildEntry)) {
			unpin(indexPage);
			return null;
		}

		BTIndexPage splitedCopy = new BTIndexPage(headerPage.get_keyType());
		entriesDivider(indexPage, splitedCopy, newChildEntry);

		RID rid = new RID();
		KeyDataEntry middle = splitedCopy.getFirst(rid);
		splitedCopy.setLeftLink(((IndexData) middle.data).getData());
		splitedCopy.deleteSortedRecord(rid);
		((IndexData) middle.data).setData(splitedCopy.getCurPage());// this line
																	// maybe
																	// wrong

		// if index page isn't the root just return
		if (headerPage.get_rootId().pid != indexPage.getCurPage().pid) {
			unpin(indexPage);
			unpin(splitedCopy);
			return middle;
		}

		BTIndexPage theRoot = new BTIndexPage(headerPage.get_keyType());
		headerPage.set_rootId(theRoot.getCurPage());
		theRoot.insertRecord(middle);
		theRoot.setLeftLink(indexPage.getCurPage());
		unpin(indexPage);
		unpin(splitedCopy);
		unpin(theRoot);

		return null;
	}

	private KeyDataEntry insertInLeafPage(BTLeafPage leaf, KeyDataEntry entry)
			throws Exception {

		if (pageInsertion(leaf, entry)) {
			unpin(leaf);
			return null;
		}

		BTLeafPage splitedCopy = new BTLeafPage(headerPage.get_keyType());
		entriesDivider(leaf, splitedCopy, entry);

		// linked list pointers setup
		splitedCopy.setNextPage(leaf.getNextPage());
		leaf.setNextPage(splitedCopy.getCurPage());
		splitedCopy.setPrevPage(leaf.getCurPage());

		if (splitedCopy.getNextPage().pid != global.GlobalConst.INVALID_PAGE) {
			PageId nexLeafID = new PageId(splitedCopy.getNextPage().pid);
			BTLeafPage nextLeafPage = new BTLeafPage(nexLeafID,
					headerPage.get_keyType());
			nextLeafPage.setPrevPage(splitedCopy.getCurPage());
			unpin(nextLeafPage);

		}

		// this sets the next of the middle entry to the splitted page
		KeyDataEntry middle = splitedCopy.getFirst(new RID());
		KeyDataEntry middleIndex = new KeyDataEntry(middle.key,
				splitedCopy.getCurPage());

		if (headerPage.get_rootId().pid != leaf.getCurPage().pid) {
			unpin(leaf);
			unpin(splitedCopy);
			return middleIndex;
		}

		// I am the ROOT
		BTIndexPage theRoot = new BTIndexPage(headerPage.get_keyType());
		headerPage.set_rootId(theRoot.getCurPage());
		theRoot.insertRecord(middleIndex);
		theRoot.setLeftLink(leaf.getCurPage());
		unpin(theRoot);
		unpin(leaf);
		unpin(splitedCopy);
		return null;

	}

	/**
	 * delete leaf entry given its pair. `rid' is IN the data entry; it is not
	 * the id of the data entry. myNote : as we are using alternative 2 , for a
	 * single key we store multiple entries with the same key but different data
	 * (rid).
	 * 
	 * @param key
	 *            - the key in pair . Input Parameter.
	 * @param rid
	 *            - the rid in pair . Input Parameter.
	 */
	@Override
	public boolean Delete(KeyClass data, RID rid) {

		try {// check if the btree is closed
			checkClosed();
		} catch (Exception e) {
			return false;
		}

		try {

			// check if there is no root in the first place
			if (headerPage.get_rootId().pid == global.GlobalConst.INVALID_PAGE)
				return false;

			// pin the root
			BTSortedPage root = pinPage(headerPage.get_rootId());
			PageId t = getLeafPage(root, data);
			BTLeafPage leaf = new BTLeafPage(t, headerPage.get_keyType());

			boolean successed = false;
			KeyDataEntry entry = null;
			RID r = new RID();

			entry = leaf.getFirst(r);
			while (entry != null) {
				if (BT.keyCompare(data, entry.key) == 0
						&& ((LeafData) entry.data).getData().equals(rid)) {
					successed = true;
					leaf.deleteSortedRecord(r);
					break;
				}
				entry = leaf.getNext(r);
			}

			unpin(leaf);
			return successed;

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	private PageId getLeafPage(BTSortedPage nodePointer, KeyClass key)
			throws Exception {

		if (nodePointer.getType() == NodeType.LEAF) {
			unpin(nodePointer);
			return nodePointer.getCurPage();
		}

		PageId subTree = ((BTIndexPage) nodePointer).getPageNoByKey(key);
		unpin(nodePointer);
		BTSortedPage child = pinPage(subTree);
		return getLeafPage(child, key);
	}

	/**
	 * Close the B+ tree file. Unpin header page.
	 */
	public void close() throws Exception {

		// remember to close the scanners
		closed = true;
		SystemDefs.JavabaseBM.unpinPage(headerPage.getCurPage(), true);
	}

	private void checkClosed() throws Exception {
		if (closed)
			throw new IllegalAccessException();
	}

	/**
	 * Destroy entire B+ tree file. delete every page and close the BTreeFile
	 * 
	 * @throws Exception
	 */
	public void destroyFile() throws Exception {

		checkClosed();
		deleteRecursive(headerPage.get_rootId());
		unpin(headerPage);
		SystemDefs.JavabaseDB.delete_file_entry(fileName);
		headerPage.set_rootId(new PageId(-1));
		SystemDefs.JavabaseBM.freePage(headerPage.getCurPage());
		// closed = true;
	}

	private void deleteRecursive(PageId id) throws Exception {

		if (id.pid == global.GlobalConst.INVALID_PAGE)
			return;

		BTSortedPage node = pinPage(id);

		if (node.getType() == NodeType.LEAF) {// you reached a leaf node just
												// delete it
			unpin(node);
			SystemDefs.JavabaseBM.freePage(id);
			return;
		}

		BTIndexPage indexPage = (BTIndexPage) node;
		KeyDataEntry entry;
		RID rid = new RID();
		entry = indexPage.getFirst(rid);
		while (entry != null) {
			deleteRecursive(((IndexData) entry.data).getData());
			entry = indexPage.getNext(rid);
		}
		deleteRecursive(indexPage.getLeftLink());
		unpin(node);
		SystemDefs.JavabaseBM.freePage(id);

	}

	/**
	 * create a scan with given keys Cases: (1) lo_key = null, hi_key = null
	 * scan the whole index (2) lo_key = null, hi_key!= null range scan from min
	 * to the hi_key (3) lo_key!= null, hi_key = null range scan from the lo_key
	 * to max (4) lo_key!= null, hi_key!= null, lo_key = hi_key exact match (
	 * might not unique) (5) lo_key!= null, hi_key!= null, lo_key < hi_key range
	 * scan from lo_key to hi_key
	 * 
	 * @param lowkey
	 * @param hikey
	 * @return
	 * @throws Exception
	 */
	public BTFileScan new_scan(KeyClass lowkey, KeyClass hikey)
			throws Exception {

		BTFileScan scan = new BTFileScan(lowkey, hikey, this, headerPage);
		return scan;
	}

	// /////////////////////////////////////////////////////////////////////////////////
	// these methods are needed in BTTest
	// what does they do ? I don't know ! but I hope I will find out what they
	// do.

	public BTreeHeaderPage getHeaderPage() throws Exception {

		checkClosed();

		return headerPage;
	}

	public void traceFilename(String string) throws Exception {
		checkClosed();

	}

	private void unpin(HFPage page) {

		int x = -1;
		try {
			x = page.getCurPage().pid;
			SystemDefs.JavabaseBM.unpinPage(page.getCurPage(), true);
		} catch (Exception e) {
			System.out.println("failed to unpin page id : " + x);
			e.printStackTrace();
		}
	}

	private BTSortedPage pinPage(PageId id) throws Exception {

		Page page = new Page();
		SystemDefs.JavabaseBM.pinPage(id, page, false);

		HFPage page2 = new HFPage(page);
		if (page2.getType() == NodeType.INDEX)
			return new BTIndexPage(page, headerPage.get_keyType());
		else
			return new BTLeafPage(page, headerPage.get_keyType());

	}

	private boolean pageInsertion(BTSortedPage page, KeyDataEntry entry) {
		try {
			if (page.insertRecord(entry) == null)
				return false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void entriesDivider(BTLeafPage leftPage, BTLeafPage rightPage,
			KeyDataEntry entry) {

		try {

			RID rid = new RID();

			KeyDataEntry currentEntry = leftPage.getFirst(rid);
			while (currentEntry != null) {
				rightPage.insertRecord(currentEntry.key,
						((LeafData) currentEntry.data).getData());
				leftPage.deleteSortedRecord(rid);
				currentEntry = leftPage.getFirst(rid);
			}

			currentEntry = rightPage.getFirst(rid);
			boolean inserted = true;
			while (leftPage.available_space() > rightPage.available_space()) {
				if (inserted && BT.keyCompare(entry.key, currentEntry.key) < 0) {
					inserted = false;
					leftPage.insertRecord(entry);
					continue;
				}

				leftPage.insertRecord(currentEntry.key,
						((LeafData) currentEntry.data).getData());
				rightPage.deleteSortedRecord(rid);
				currentEntry = rightPage.getFirst(rid);
			}

		} catch (Exception e) {

		}

	}

	private void entriesDivider(BTIndexPage leftPage, BTIndexPage rightPage,
			KeyDataEntry entry) {

		try {

			RID rid = new RID();

			KeyDataEntry currentEntry = leftPage.getFirst(rid);
			while (currentEntry != null) {
				rightPage.insertKey(currentEntry.key,
						((IndexData) currentEntry.data).getData());
				leftPage.deleteSortedRecord(rid);
				currentEntry = leftPage.getFirst(rid);
			}

			currentEntry = rightPage.getFirst(rid);
			boolean inserted = true;
			while (leftPage.available_space() > rightPage.available_space()) {
				if (inserted && BT.keyCompare(entry.key, currentEntry.key) < 0) {
					inserted = false;
					leftPage.insertRecord(entry);
					continue;
				}

				leftPage.insertKey(currentEntry.key,
						((IndexData) currentEntry.data).getData());
				rightPage.deleteSortedRecord(rid);
				currentEntry = rightPage.getFirst(rid);
			}

		} catch (Exception e) {

		}

	}

}