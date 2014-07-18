package com.chacha.dao.multimedia.impl;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.Work;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import com.chacha.dao.multimedia.QueryMultimediaDAO;
import com.chacha.document.multimedia.QueryMultimediaElement;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * @author Jon Strayer
 * 
 */
public class HibernateQueryMultimediaDAO extends HibernateDaoSupport implements QueryMultimediaDAO
{
	String MIME_SELECT = "select mime_type from mime_type where id = ?";
	String MIME_FIND = "select id from mime_type where mime_type = ? ";
	String MIME_INSERT = "insert into mime_type (mime_type) values(?) ";

	public HibernateQueryMultimediaDAO(final SessionFactory factory)
	{
		super.setSessionFactory(factory);
	}

	@Override
	public void save(final QueryMultimediaElement element)
	{
		if (element.getMimeType() == null)
		{
			setMimeTypeId(element);
		}

		getHibernateTemplate().save(element);

	}

	@Override
	public QueryMultimediaElement findById(final Long qhid)
	{
		final QueryMultimediaElement result = getHibernateTemplate().get(QueryMultimediaElement.class, qhid);
		if (result != null)
		{
			setMimeTypeString(result);
		}
		return result;
	}

	private void setMimeTypeId(final QueryMultimediaElement element)
	{
		final String type = element.getMimeString();
		if (type != null)
		{
			final Integer id = getId(type);
			if (id != null)
			{
				element.setMimeType(id);
			}
			else
			{
                getHibernateTemplate().execute(new HibernateCallback<Void>() {
                    @Override
                    public Void doInHibernate(Session session) throws HibernateException, SQLException {
                        session.doWork(new Work() {

                            @Override
                            public void execute(final Connection con) throws SQLException {
                                final PreparedStatement state = con.prepareStatement(MIME_INSERT);
                                state.setString(1, type);
                                state.execute();

                            }
                        });

                        return null;  //To change body of implemented methods use File | Settings | File Templates.
                    }
                });

				element.setMimeType(getId(type));
			}
		}
	}

	private Integer getId(final String mimeType)
	{
        return getHibernateTemplate().execute(new HibernateCallback<Integer>() {
            @Override
            public Integer doInHibernate(Session session) throws HibernateException, SQLException {
                SQLQuery query = session.createSQLQuery(MIME_FIND);
                query.setString(0, mimeType);

                final List<Short> results = query.list();
                if (results.size() == 1)
                {
                    return results.get(0).intValue();
                } else
                {
                    return null;
                }
            }
        });
	}

	private void setMimeTypeString(final QueryMultimediaElement element)
	{
		final Integer mimeId = element.getMimeType();
		if (mimeId != null)
		{
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(Session session) throws HibernateException, SQLException {
                    SQLQuery query = session.createSQLQuery(MIME_SELECT);
                    query.setLong(0, mimeId);
                    final List<String> results = query.list();
                    if (results.size() == 1)
                    {
                        element.setMimeString((String) results.get(0));
                    }

                    return null;
                }
            });
		}
	}

	@Configuration
	public static class TestConfiguration
	{

		@Bean
		public DataSource dataSource()
		{
			final ComboPooledDataSource dataSource = new ComboPooledDataSource();

			try
			{
				dataSource.setDriverClass("com.mysql.jdbc.Driver");
			} catch (final PropertyVetoException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dataSource.setJdbcUrl("jdbc:mysql://ums_cldb01.Dev2.chacha.local:3306/query_history");
			dataSource.setUser("app_psp_rw");
			dataSource.setPassword("jhrt67gpo8w54t");
			return dataSource;
		}

		@Bean
		AnnotationSessionFactoryBean sessionFactory()
		{
			final AnnotationSessionFactoryBean factory = new AnnotationSessionFactoryBean();
			factory.setDataSource(dataSource());
			final Class[] annotatedClasses =
			{
					QueryMultimediaElement.class
			};
			factory.setAnnotatedClasses(annotatedClasses);
			final Properties props = new Properties();
			props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
			props.put("hibernate.show_sql", "true");
			props.put("hibernate.cache.use_second_level_cache", "false");
			factory.setHibernateProperties(props);
			return factory;
		}

		@Bean
		HibernateQueryMultimediaDAO dao()
		{
			return new HibernateQueryMultimediaDAO(sessionFactory().getObject());
		}

	}

	public static void main(final String[] notUsed) throws Exception
	{
		// TestConfiguration config = new TestConfiguration();
		final AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(TestConfiguration.class);
		ctx.refresh();
		final Long id = 2001L;
		final String key = "9df676f3c606272305cd5eaea55b631a61f33ddb";
		final String mimeType = "image/jpg";
		final HibernateQueryMultimediaDAO dao = ctx.getBean(HibernateQueryMultimediaDAO.class);
		/*
		 * QueryMultimediaElement element = new QueryMultimediaElement();
		 * 
		 * element.setStringKey("9df676f3c606272305cd5eaea55b631a61f33ddb");
		 * element.setMimeString("image/jpg"); element.setQhid(2001L);
		 * element.setType(MultimediaType.image); dao.save(element);
		 */
		final QueryMultimediaElement newElement = dao.findById(id);
		final Long newId = newElement.getQhid();
		final String newKey = newElement.getStringKey();
		final String newMime = newElement.getMimeString();

		System.out.println("id: " + newId + " " + newId.equals(id));
		System.out.println("Key: " + newKey + " " + newKey.equals(key));
		System.out.println("Mime type: " + newMime + " " + newMime.equals(mimeType));
	}
}
