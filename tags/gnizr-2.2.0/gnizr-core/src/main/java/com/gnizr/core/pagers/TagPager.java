/*
 * gnizr is a trademark of Image Matters LLC in the United States.
 * 
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either expressed or implied. See the License
 * for the specific language governing rights and limitations under the License.
 * 
 * The Initial Contributor of the Original Code is Image Matters LLC.
 * Portions created by the Initial Contributor are Copyright (C) 2007
 * Image Matters LLC. All Rights Reserved.
 */
package com.gnizr.core.pagers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gnizr.core.exceptions.MissingIdException;
import com.gnizr.core.exceptions.NoSuchTagException;
import com.gnizr.core.exceptions.NoSuchUserException;
import com.gnizr.core.exceptions.NoSuchUserTagException;
import com.gnizr.core.util.GnizrDaoUtil;
import com.gnizr.db.GnizrDao;
import com.gnizr.db.dao.TagAssertion;
import com.gnizr.db.dao.TagProperty;
import com.gnizr.db.dao.User;
import com.gnizr.db.dao.UserTag;
import com.gnizr.db.dao.tag.TagAssertionDao;
import com.gnizr.db.dao.tag.TagDao;
import com.gnizr.db.dao.tag.TagPropertyDao;
import com.gnizr.db.dao.user.UserDao;

public class TagPager implements Serializable {

	private static final long serialVersionUID = 3602883841342374804L;

	private static final Logger logger = Logger.getLogger(TagPager.class);

	private UserDao userDao;

	private TagDao tagDao;

	private TagPropertyDao tagPropertyDao;

	private TagAssertionDao tagAssertionDao;

	private TagProperty skosRelatedPrpt;

	private TagProperty skosNarrowerPrpt;

	private TagProperty skosBroaderPrpt;

	private TagProperty rdfTypePrpt;

	public TagPager(GnizrDao gnizrDao) {
		this.userDao = gnizrDao.getUserDao();
		this.tagDao = gnizrDao.getTagDao();
		this.tagPropertyDao = gnizrDao.getTagPropertyDao();
		this.tagAssertionDao = gnizrDao.getTagAssertionDao();
		initTagProperty();
	}

	private void initTagProperty() {
		rdfTypePrpt = tagPropertyDao.getTagProperty("rdf","type");
		skosRelatedPrpt = tagPropertyDao.getTagProperty("skos","related");
		skosNarrowerPrpt = tagPropertyDao.getTagProperty("skos","narrower");
		skosBroaderPrpt = tagPropertyDao.getTagProperty("skos","broader");
	}

	public List<UserTag> findRDFType(User user, UserTag userTag) throws NoSuchUserException, NoSuchTagException, NoSuchUserTagException, MissingIdException{
		logger.debug("findRDFType: user="+user+",userTag="+userTag);
		if (GnizrDaoUtil.hasMissingId(user)) {
			GnizrDaoUtil.fillId(userDao, user);
		}
		if (GnizrDaoUtil.hasMissingId(userTag)) {
			GnizrDaoUtil.fillId(tagDao, userDao, userTag);
		}
		List<TagAssertion> asrts = null;
		List<UserTag> classTags = new ArrayList<UserTag>();
		asrts = tagAssertionDao.findTagAssertion(user, userTag,
				rdfTypePrpt, null);	
		if(asrts != null && !asrts.isEmpty()){
			addUserTagObjectToList(asrts,classTags,-1);
		}
		return classTags;		
	}
	
	public List<UserTag> findSKOSRelated(User user, UserTag userTag)
			throws NoSuchUserException, NoSuchTagException,
			NoSuchUserTagException, MissingIdException {
		logger.debug("findSKOSRelated: user=" + user + ",userTag=" + userTag);
		if (GnizrDaoUtil.hasMissingId(user)) {
			GnizrDaoUtil.fillId(userDao, user);
		}
		if (GnizrDaoUtil.hasMissingId(userTag)) {
			GnizrDaoUtil.fillId(tagDao, userDao, userTag);
		}
		List<TagAssertion> asrts = null;
		List<UserTag> relatedTags = new ArrayList<UserTag>();
		asrts = tagAssertionDao.findTagAssertion(user, userTag,
				skosRelatedPrpt, null);
		if (asrts != null && !asrts.isEmpty()) {
			addUserTagObjectToList(asrts, relatedTags,userTag.getId());
		}
		asrts = tagAssertionDao.findTagAssertion(user, null, skosRelatedPrpt,
				userTag);
		if (asrts != null && !asrts.isEmpty()) {
			addUserTagSubjectToList(asrts, relatedTags,userTag.getId());
		}

		return relatedTags;
	}

	public List<UserTag> findSKOSBroader(User user, UserTag userTag)
			throws NoSuchUserException, NoSuchTagException,
			NoSuchUserTagException, MissingIdException {
		logger.debug("findSKOSBroader: user=" + user + ",userTag=" + userTag);
		if (GnizrDaoUtil.hasMissingId(user)) {
			GnizrDaoUtil.fillId(userDao, user);
		}
		if (GnizrDaoUtil.hasMissingId(userTag)) {
			GnizrDaoUtil.fillId(tagDao, userDao, userTag);
		}
		List<UserTag> result = new ArrayList<UserTag>();
		seekBroader(user, userTag, result,userTag.getId());		
		return result;
	}

	private void seekBroader(User user, UserTag subject, List<UserTag> result, int sourceTagId) {
		List<UserTag> tempResult = new ArrayList<UserTag>();
		try {
			List<TagAssertion> aList = tagAssertionDao.findTagAssertion(user,
					subject, skosBroaderPrpt, null);
			addUserTagObjectToList(aList, tempResult,sourceTagId);
			
			aList = tagAssertionDao.findTagAssertion(user,null,skosNarrowerPrpt, subject);
			addUserTagSubjectToList(aList, tempResult,sourceTagId);
			
			for (Iterator<UserTag> it = tempResult.iterator(); it.hasNext();) {
				UserTag next2seek = it.next();
				if (result.contains(next2seek) == false) {
					result.add(next2seek);
					seekBroader(user, next2seek, result,sourceTagId);
				}
			}			
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public List<UserTag> findSKOSNarrower(User user, UserTag userTag)
			throws NoSuchUserException, NoSuchTagException,
			NoSuchUserTagException, MissingIdException {
		logger.debug("findSKOSNarrower: user=" + user + ",userTag=" + userTag);
		if (GnizrDaoUtil.hasMissingId(user)) {
			GnizrDaoUtil.fillId(userDao, user);
		}
		if (GnizrDaoUtil.hasMissingId(userTag)) {
			GnizrDaoUtil.fillId(tagDao, userDao, userTag);
		}
		List<UserTag> result = new ArrayList<UserTag>();
		seekNarrower(user, userTag, result, userTag.getId());			
		return result;
	}

	private void seekNarrower(User user, UserTag subject, List<UserTag> result, int sourceTagId) {
		List<UserTag> tempResult = new ArrayList<UserTag>();
		try {
			List<TagAssertion> aList = tagAssertionDao.findTagAssertion(user,
					subject, skosNarrowerPrpt, null);
			addUserTagObjectToList(aList, tempResult, sourceTagId);
			
			aList = tagAssertionDao.findTagAssertion(user,null,skosBroaderPrpt, subject);
			addUserTagSubjectToList(aList, tempResult, sourceTagId);
			
			for (Iterator<UserTag> it = tempResult.iterator(); it.hasNext();) {
				UserTag next2seek = it.next();
				if (result.contains(next2seek) == false) {
					result.add(next2seek);
					seekNarrower(user, next2seek, result, sourceTagId);
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * Returns a list of class tags. A class tag is <code>UserTag</code>
	 * that is the "object" in a tag assertion, in which the "predict" is 
	 * "type".
	 * 
	 * @param user
	 * @return a list of <code>UserTag</code> objects that are class tags.
	 * @throws NoSuchUserException the input user doesn't exist in the system
	 */
	public List<UserTag> findUserTagClass(User user) throws NoSuchUserException {
		logger.debug("findUserTagClass: user=" + user);
		if (GnizrDaoUtil.hasMissingId(user)) {
			GnizrDaoUtil.fillId(userDao, user);
		}
		List<TagAssertion> asrts = null;
		List<UserTag> classTags = new ArrayList<UserTag>();
		asrts = tagAssertionDao.findTagAssertion(user, null, rdfTypePrpt, null);
		if (asrts != null && !asrts.isEmpty()) {
			addUserTagObjectToList(asrts, classTags,-1);
		}
		return classTags;
	}
	
	public List<UserTag> findUserTagInstance(User user, UserTag classTag) throws NoSuchUserException, NoSuchTagException, NoSuchUserTagException, MissingIdException{
		logger.debug("findUserTagInstance: user="+user+",classTag="+classTag);
		if(GnizrDaoUtil.hasMissingId(user)){
			GnizrDaoUtil.fillId(userDao, user);
		}
		if(GnizrDaoUtil.hasMissingId(classTag)){
			GnizrDaoUtil.fillId(tagDao, userDao, classTag);
		}
		List<UserTag> instanceTag = new ArrayList<UserTag>();
		List<TagAssertion> asrts = tagAssertionDao.findTagAssertion(user,null,rdfTypePrpt,classTag);
		if(asrts != null && !asrts.isEmpty()){
			addUserTagSubjectToList(asrts,instanceTag,classTag.getId());
		}		
		return instanceTag;
	}
	

	private void addUserTagObjectToList(List<TagAssertion> tagAssertions,
			List<UserTag> userTags, int ignoreTagId) {
		for (Iterator<TagAssertion> it = tagAssertions.iterator(); it.hasNext();) {
			TagAssertion ta = it.next();
			UserTag ut = ta.getObject();
			if (ut != null) {
				if (ut.getId() != ignoreTagId && userTags.contains(ut) == false) {
					userTags.add(ut);
				}
			} else {
				logger
						.error("addUserTagObjectToList() finds TagAssertion.getObject() to return NULL.");
			}
		}
	}

	private void addUserTagSubjectToList(List<TagAssertion> tagAssertions,
			List<UserTag> userTags, int ignoreTagId) {
		for (Iterator<TagAssertion> it = tagAssertions.iterator(); it.hasNext();) {
			TagAssertion ta = it.next();
			UserTag ut = ta.getSubject();
			if (ut != null) {
				if (ut.getId() != ignoreTagId && userTags.contains(ut) == false) {
					userTags.add(ut);
				}
			} else {
				logger
						.error("addUserTagSubjectToList() finds TagAssertion.getSubject() to return NULL.");
			}
		}
	}

	public Map<String, List<UserTag>> getRDFTypeTagGroups(User user) throws NoSuchUserException{
		GnizrDaoUtil.fillId(userDao, user);
		Map<String,List<UserTag>> map = new HashMap<String, List<UserTag>>();
		List<TagAssertion> result = tagAssertionDao.findTagAssertion(user,null,rdfTypePrpt,null);
		for(TagAssertion t : result){
			String tagGroupName = t.getObject().getTag().getLabel();
			List<UserTag> memberList = map.get(tagGroupName);
			if(memberList == null){
				memberList = new ArrayList<UserTag>();
				map.put(tagGroupName,memberList);
			}
			memberList.add(t.getSubject());
		}
		return map;
	}
	
}
