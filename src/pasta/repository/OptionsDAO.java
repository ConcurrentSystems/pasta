/*
MIT License

Copyright (c) 2012-2017 PASTA Contributors (see CONTRIBUTORS.txt)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package pasta.repository;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import pasta.domain.options.Option;

@Transactional
@Repository("optionsDAO")
public class OptionsDAO extends BaseDAO {

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private SessionFactory sessionFactory;
	
	public void delete(String key) {
		Option option = getOption(key);
		delete(option);
	}
	
	@SuppressWarnings("unchecked")
	public List<Option> getAllOptions() {
		return sessionFactory.getCurrentSession()
				.createCriteria(Option.class)
				.addOrder(Order.asc("key"))
				.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<Option> getAllOptionsStartingWith(String prefix) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(Option.class);
		cr.add(Restrictions.like("key", prefix, MatchMode.START));
		cr.addOrder(Order.asc("key"));
		return cr.list();
	}
	
	public Option getOption(String key) {
		Criteria cr = sessionFactory.getCurrentSession().createCriteria(Option.class);
		cr.add(Restrictions.eq("key", key));
		return (Option) cr.uniqueResult();
	}
	
	public boolean hasOption(String key) {
		return getOption(key) != null;
	}
}
