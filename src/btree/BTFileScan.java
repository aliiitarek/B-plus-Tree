package btree;
/**
 *  @author AlyTarek & Ahmed Hussien
 * */


import java.io.IOException;

import diskmgr.Page;
import global.*;
import btree.BT;
import bufmgr.HashEntryNotFoundException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;

public class BTFileScan extends IndexFileScan implements GlobalConst {

	// Private instance Variables
	private int keySize;
	private int keyType;
	private KeyClass startKey, endKey;
	private boolean beforeFirstCall, beforeDelete;
	private BTreeFile bTreeFile;
	private RID currentRID;
	private BTLeafPage currentLeafPage;
	private BTreeHeaderPage head;

	// String treeFilename;

	public BTFileScan(KeyClass lo_key, KeyClass hi_key, BTreeFile file,
			BTreeHeaderPage headerPage) throws Exception {

		startKey = lo_key;
		endKey = hi_key;
		bTreeFile = file;
		beforeFirstCall = true;
		beforeDelete = true;
		currentRID = new RID();
		head = headerPage;
		keyType = headerPage.get_keyType();
		keySize = headerPage.get_maxKeySize();
		if (headerPage.get_rootId().pid == INVALID_PAGE) {
			currentLeafPage = null;
		} else {
			currentLeafPage = getPageOfFirstRecord();
		}
	}

	@Override
	public KeyDataEntry get_next() throws Exception {

		/*
		 * If the left most leafPage is null for any reason stated in
		 * getPageOfFirstRecord() then "we handle this" by returning null
		 * because there are no records to scan NO UNPINNING IS REQUIRED HERE
		 */
		if (currentLeafPage == null)
			return null;

		KeyDataEntry returnedEntry = null;
		PageId pageId = new PageId();

		/*
		 * if (we haven't called getNext before and neither called
		 * DeleteCurrent) OR* (we deleted the current record so the next one is
		 * still pointed by currentRID) A CONVENTION WE USED IS THAT : We can't
		 * delete a record until it's scanned first
		 */
		if ((beforeFirstCall && beforeDelete)
				|| (!beforeDelete && !beforeFirstCall)) {
			beforeDelete = true; // we scanned the entry now so it still
									// isn't deleted
			beforeFirstCall = false;// we called the get method so
									// beforeFirstCall is False
			returnedEntry = currentLeafPage.getCurrent(currentRID);
		} else {
			// else we get the next entry in this leaf page
			returnedEntry = currentLeafPage.getNext(currentRID);
		}
		/*
		 * Now this leafPage may have finished all records in it so we need to
		 * go to the successive leafPage
		 */
		while (returnedEntry == null) {
			pageId = currentLeafPage.getNextPage();
			// we have to unpin the currentLeafPage either way so we do it
			// now
			SystemDefs.JavabaseBM.unpinPage(currentLeafPage.getCurPage(), true);
			if (pageId.pid == INVALID_PAGE) {
				currentLeafPage = null;
				return null;
			}
			/*
			 * Since this next leafPage is Valid , we now pin it and make it our
			 * currentLeafPage
			 */
			currentLeafPage = new BTLeafPage(pageId, keyType);
			/*
			 * now we set our entry to the first record in this new Page -hoping
			 * its not empty- and setting the currentRID with the rid of this
			 * first record
			 */
			returnedEntry = currentLeafPage.getFirst(currentRID);
		}

		/* THE STOPPING CONDITION */
		if (endKey != null) { // if we end at a specific endKey not the end
								// of the LeafPages
			if (BT.keyCompare(endKey, returnedEntry.key) < 0) {
				/*
				 * if we passed the maxLimit (ie the endKey value)
				 */
				// System.out.println(SystemDefs.JavabaseBM.getNumUnpinnedBuffers()+" NumUnPinnedBuffers");

				SystemDefs.JavabaseBM.unpinPage(currentLeafPage.getCurPage(),
						false);
				currentLeafPage = null;
				/*
				 * false because delete method stores the modifications
				 */
				return null;
			}
		}
		/* return Required Valid Entry */
		return returnedEntry;

	}

	@Override
	public void delete_current() throws Exception {
		/*
		 * Illigal Delete because either : You haven't scanned any entry yet -
		 * inorder to delete OR You already deleted Scanned entry
		 */
		if (beforeFirstCall || !beforeDelete)
			return;

		/*
		 * What if the leaf page equals null ie no entries to even delete or
		 * work with This should never happen , if it happens means there is an
		 * error in the logic of scan so we Throw an error
		 */
		if (currentLeafPage == null) {
			throw new Exception(
					"leaf page equals null ie no entries to even delete or \nwork with This should never happen");
		}

		/* Now we try to delete the required scanned entry */

		KeyDataEntry currentEntry, nextEntry;
		RID ridCopy = new RID();
		ridCopy.copyRid(currentRID);
		currentEntry = currentLeafPage.getCurrent(currentRID);
		nextEntry = currentLeafPage.getNext(ridCopy);
		bTreeFile.Delete(currentEntry.key,
				((LeafData) currentEntry.data).getData());
		beforeFirstCall = false;
		beforeDelete = false;

	}

	@Override
	public int keysize() {
		return keySize;
	}

	public void DestroyBTreeFileScan() throws Throwable {
		bTreeFile = null;
		SystemDefs.JavabaseBM.unpinPage(currentLeafPage.getCurPage(), false);
		/*
		 * false because delete method stores the modifications
		 */
		currentLeafPage = null;
		currentRID = null;
		keySize = -1;
		keyType = -1;
		startKey = null;
		endKey = null;
		this.finalize();
	}

	private BTLeafPage getPageOfFirstRecord() throws Exception {
		// currentRID , startKey
		BTSortedPage sortedPage;
		BTLeafPage leafPage;
		BTIndexPage indexPage;
		Page aPage = new Page();
		PageId pageId, LeftMostPageId, nextLeafPageId;
		KeyDataEntry currentEntry;

		/* Load the Root Page */
		pageId = head.get_rootId();

		if (pageId.pid == INVALID_PAGE) { // no Root --> BTree is
			// Empty
			leafPage = null;
			return null;
		}

		SystemDefs.JavabaseBM.pinPage(pageId, aPage, false);

		/*
		 * Now create a SuperClass SortedPage because you don't know whether
		 * aPage is leaf or index
		 */
		sortedPage = new BTSortedPage(aPage, keyType);

		/*
		 * Now Our Algorithm is to Find the left most Leaf page containing the
		 * Lo_Key So if the root was an Index page we have to traverse till we
		 * reach a Leaf page
		 */

		while (NodeType.INDEX == sortedPage.getType()) { // still in index page
			/* load aPage as an indexPage */
			indexPage = new BTIndexPage(aPage, keyType);

			/*
			 * Get left-most-page just-in-case our startKey is Smaller than the
			 * first record on this indexPage
			 */
			LeftMostPageId = indexPage.getPrevPage();
			currentEntry = indexPage.getFirst(currentRID);
			System.out.println("Outer Loop");
			while (currentEntry != null && startKey != null
					&& BT.keyCompare(currentEntry.key, startKey) < 0) {
				/*
				 * if StartKey Bigger than we still need to find the correct
				 * pointer to lower level layers
				 */
				System.out.println("Inner Loop");

				LeftMostPageId = ((IndexData) currentEntry.data).getData();
				/*
				 * leftMostPgId contains the next pointer on this index page
				 */
				currentEntry = indexPage.getNext(currentRID);
			}

			/* unpin the current index page */
			SystemDefs.JavabaseBM.unpinPage(pageId, false);

			/*
			 * and load the next left most page (either index or leaf) so we
			 * load it as a sortedPage
			 */
			pageId = LeftMostPageId;
			SystemDefs.JavabaseBM.pinPage(pageId, aPage, false);
			sortedPage = new BTSortedPage(aPage, keyType);
		}

		/*
		 * Now we are sure that the Page ="aPage" contains the leaf page desired
		 */
		leafPage = new BTLeafPage(aPage, keyType);
		currentEntry = leafPage.getFirst(currentRID);

		/*
		 * Now since we delete record without merging then we might get across
		 * an (partially)empty tree with index nodes and leaf nodes so we need
		 * to handle this case
		 */
		while (currentEntry == null) {
			System.out.println("CurEntry = null");
			nextLeafPageId = leafPage.getNextPage();
			SystemDefs.JavabaseBM.unpinPage(pageId, false);
			if (nextLeafPageId.pid == INVALID_PAGE) {
				/*
				 * then no more leafpages after this current leaf page therefore
				 * no nodes to traverse on (tree empty)
				 */
				return null;
			}
			pageId = nextLeafPageId;
			SystemDefs.JavabaseBM.pinPage(pageId, aPage, false);
			leafPage = new BTLeafPage(aPage, keyType);
			currentEntry = leafPage.getFirst(currentRID);
		}

		/*
		 * given the definition of the scan method if startKey is = null then we
		 * should start from the first non-empty leaf page till the endKey and
		 * since now we have the first non-empty leaf page pinned and the
		 * "leafPage" is pointing to that page so we should return this if the
		 * startKey == null
		 */
		if (startKey == null) {
			return leafPage;
		}
		/*
		 * In the end, we need to set currentRID to the correct record in the
		 * current leafPage
		 */
		while (BT.keyCompare(currentEntry.key, startKey) < 0) {
			currentEntry = leafPage.getNext(currentRID);
			// currentEntry = leafPage.getCurrent(currentRID);
			while (currentEntry == null) { // head to the next leafPage to the
											// right
				nextLeafPageId = leafPage.getNextPage();
				SystemDefs.JavabaseBM.unpinPage(pageId, false);

				if (nextLeafPageId.pid == INVALID_PAGE) {
					/*
					 * can't scan (tree with no such key)
					 */
					return null;
				}

				pageId = nextLeafPageId;
				SystemDefs.JavabaseBM.pinPage(pageId, aPage, false);
				leafPage = new BTLeafPage(aPage, keyType);

				currentEntry = leafPage.getFirst(currentRID);
			}
		}

		return leafPage;
	}

}