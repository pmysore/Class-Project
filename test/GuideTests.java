package com.chacha.rm.guide;

import java.util.Date;

import org.apache.camel.Endpoint;
import org.apache.camel.ProducerTemplate;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.chacha.document.annotated.Suggestion;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("GuideTests.xml")
public class GuideTests {
	
	@Autowired
	private ProducerTemplate producerTemplate;
	
	@Autowired
	private Endpoint sugpEndpoint;
	
	@Test
	public void testSugpRemove(){
        Suggestion suggestion = new Suggestion();

        suggestion.setId(String.format("%s-HASHTAG", "whatishappening"));
        suggestion.setType("HASHTAG");
        suggestion.setText("whatishappening");
        suggestion.setFrequency(0);
        suggestion.setPg(true);
        suggestion.setUpdatedAt(new Date());
        
        producerTemplate.sendBody(sugpEndpoint, suggestion);
	}
	
	@Ignore
	@Test
	public void testSugpAdd(){
        Suggestion suggestion = new Suggestion();

        suggestion.setId(String.format("%s-HASHTAG", "Whatishappening"));
        suggestion.setType("HASHTAG");
        suggestion.setText("Whatishappening");
        suggestion.setFrequency(0);
        suggestion.setPg(true);
        suggestion.setUpdatedAt(new Date());
        
        producerTemplate.sendBody(sugpEndpoint, suggestion);
	}
}
