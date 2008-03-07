package com.gnizr.web.action.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

import com.gnizr.core.bookmark.BookmarkPager;
import com.gnizr.core.search.DocumentCreator;
import com.gnizr.core.search.SearchIndexManager;
import com.gnizr.db.dao.Bookmark;
import com.gnizr.db.dao.DaoResult;
import com.gnizr.db.dao.User;
import com.gnizr.web.action.AbstractLoggedInUserAction;

/**
 * <p>An Action implementation for building search index of bookmarks 
 * saved by the gnizr users. This action should be accessible only to
 * the superuser of the gnizr system (i.e., the user <code>gnizr</code>).</p>
 * <p>The enforcement of this logic is done by using the proper
 * interceptor configuration in the XWork configuration.
 * </p>
 * 
 * @author Harry Chen
 * @since 2.4
 */
public class IndexBookmark extends AbstractLoggedInUserAction{

	private static final long serialVersionUID = 7092403721954833011L;
	private static final Logger logger = Logger.getLogger(IndexBookmark.class);
	
	private SearchIndexManager searchIndexManager;
	private BookmarkPager bookmarkPager;
	
	public SearchIndexManager getSearchIndexManager() {
		return searchIndexManager;
	}

	public void setSearchIndexManager(SearchIndexManager searchIndexManager) {
		this.searchIndexManager = searchIndexManager;
	}

	public BookmarkPager getBookmarkPager() {
		return bookmarkPager;
	}

	public void setBookmarkPager(BookmarkPager bookmarkPager) {
		this.bookmarkPager = bookmarkPager;
	}

	@Override
	protected boolean isStrictLoggedInUserMode() {
		return true;
	}

	@Override
	protected String go() throws Exception {
		logger.debug("IndexBookmark action go() is called.");
		resolveUser();
		
		List<User> users = userManager.listUsers();
		for(User user : users){
			logger.debug("Indexing the bookmarks of user: " + user.getUsername());
			int ppc = 10;
			int start = 0;
			int numOfPages = bookmarkPager.getMaxPageNumber(user,ppc);
			for(int i = 0; i < numOfPages; i++){
				logger.debug("--> page=" + i + ", start="+start+", ppc=" + ppc);
				DaoResult<Bookmark> result  = bookmarkPager.pageBookmark(user,start,ppc);
				doIndex(result.getResult());
				start = start + ppc;
			}
		}
		return SUCCESS;
	}

	private void doIndex(List<Bookmark> bookmarks){
		try{
			List<Document> docs = new ArrayList<Document>();
			for(Bookmark b : bookmarks){
				Document d = DocumentCreator.createDocument(b);
				if(d != null){
					docs.add(d);
				}
			}			
			logger.debug("Created Document from Bookmarks. Total number = " + docs.size());
			for(Document d : docs){
				searchIndexManager.deleteIndex(d);
				searchIndexManager.addIndex(d);
			}
			logger.debug("Requested the SearchIndexManager to update index.");
		}catch(Exception e){
			logger.error("IndexBookmark.doIndex(), "+e);
		}
	}
	
}