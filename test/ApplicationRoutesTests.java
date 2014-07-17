package com.chacha.persistence.routing;

import com.chacha.camel.ChaHeaders;
import com.chacha.camel.test.RouteTestSupport;
import com.chacha.dao.home.EventCountDao;
import com.chacha.dao.question.TextIdentifierDAO;
import com.chacha.dao.session.PriorityQuestionQueueDAO;
import com.chacha.document.ChaMessage;
import com.chacha.document.ChaSession;
import com.chacha.document.annotated.ResponseCode;
import com.chacha.document.annotated.ResponseMessage;
import com.chacha.document.answer.event.AnswerApprovedEvent;
import com.chacha.document.answer.event.AnswerStateChangeEvent;
import com.chacha.document.follow.UnfollowQuestionsRequest;
import com.chacha.document.question.Question;
import com.chacha.document.question.QuestionReuseStateChangeRequest;
import com.chacha.document.question.event.QuestionStateChangeEvent;
import com.chacha.document.session.QueryResponse;
import com.chacha.document.session.QueryUpdateRequest;
import com.chacha.document.session.QueryUpdateRequest.UpdateType;
import com.chacha.document.session.event.QueryPersistedEvent;
import com.chacha.document.session.event.QueryUpdatedEvent;
import com.chacha.document.topic.Topic;
import com.chacha.document.user.User;
import com.chacha.persistence.beans.PersistedQueryResponse;
import com.chacha.persistence.service.AutomatedPersistenceService;
import com.chacha.persistence.service.ConversationPersistenceService;
import com.chacha.persistence.service.QueryUpdateService;
import com.chacha.persistence.service.QuestionUpdateService;
import com.chacha.persistence.service.impl.StandardReuseEventProcessingService;
import com.google.common.collect.ImmutableMap;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationRoutesTests extends RouteTestSupport
{
	@Mock
	EventCountDao eventCountDao;
	
	@Mock
	PriorityQuestionQueueDAO priorityQueueDao;
	
	@Mock
	ConversationPersistenceService conversationPersistenceService;

	@Mock
	AutomatedPersistenceService automatedPersistenceService;
	
	@Mock
	QuestionUpdateService questionUpdateService;
	
	@Mock 
	QueryUpdateService queryUpdateService;
	
	@Mock
	StandardReuseEventProcessingService processingService;

	@Mock
	TextIdentifierDAO textIdentifierDAO;
	
	@EndpointInject(uri="mock:unfollow.question")
	MockEndpoint unfollowQuestion;

	@EndpointInject(uri="mock:event.query.edit")
	MockEndpoint queryEditEndpointMock;

	@EndpointInject(uri="mock:bus.question.new")
	MockEndpoint newQueryEndpointMock;
	
	@EndpointInject(uri="mock:qri.question.state.change")
	MockEndpoint qriStateChange;
	
	@EndpointInject(uri="mock:bus.query.updated")
	MockEndpoint queryUpdatedEventEndpoint;
	
	@EndpointInject(uri="mock:return")
	MockEndpoint mockReturn;
	
	@Test 
	public void resubmitInitTest() throws Exception{
		when(questionUpdateService.getQid(42L)).thenReturn(52L);
		User user = new User();
		user.setId(553L);
		
		ChaSession session = new ChaSession();
		session.setUser(user);
		session.getTransientProperties().put(ChaHeaders.EDIT_QUESTION_QHID, "42");
		
		ChaMessage cm = new ChaMessage();
		cm.setSession(session);
		
		unfollowQuestion.expectedMessageCount(1);

		template.sendBodyAndHeader("{{messaging.endpoint.session.initialize.async.receive}}", cm, ChaHeaders.EDIT_QUESTION_QHID, "42");

		verify(automatedPersistenceService).initialize(same(cm));
		verify(questionUpdateService).toggleQueryHistoryWithString(eq(42L), eq("HIDE"));
		verify(questionUpdateService).getQid(eq(42L));
		unfollowQuestion.assertIsSatisfied();

		Exchange ex = unfollowQuestion.getExchanges().get(0);
		Object foo = ex.getIn().getBody();
		assertTrue(foo instanceof UnfollowQuestionsRequest);
		Long[] ids = ((UnfollowQuestionsRequest) foo).getIds();
		assertEquals(1, ids.length);
		assertEquals(Long.valueOf(52L), ids[0]);
		assertEquals(Long.valueOf(553L), ((UnfollowQuestionsRequest) foo).getUserId());
	}

	@Test 
	public void resubmitCompleteTest() throws Exception
	{
		User user = new User();
		user.setId(55l);

		ChaSession session = new ChaSession();
		session.setUser(user);
		session.getTransientProperties().put(ChaHeaders.EDIT_QUESTION_QHID, "42");
		session.setTopics(new HashSet<Topic>());

		QueryResponse qr = new QueryResponse();
		qr.setQhid(56l);
		qr.setQuestion(new Question());
		qr.getQuestion().setId(667l);
		qr.setSession(session);
		qr.setType(QueryResponse.Type.BROADCASTED.name());


		PersistedQueryResponse persistedQueryResponse = new PersistedQueryResponse();
		persistedQueryResponse.setQueryResponse(qr);
		persistedQueryResponse.setNewQuestion(true);

		when(this.automatedPersistenceService.finalize(same(qr))).thenReturn(persistedQueryResponse);

		newQueryEndpointMock.expectedMessageCount(1);
		queryEditEndpointMock.expectedMessageCount(1);

		template.sendBodyAndHeader("direct:session.persist", qr, ChaHeaders.EDIT_QUESTION_QHID, "42");

		verify(automatedPersistenceService).finalize(same(qr));
		newQueryEndpointMock.assertIsSatisfied();
		queryEditEndpointMock.assertIsSatisfied();

		Exchange ex = queryEditEndpointMock.getExchanges().get(0);
		Object foo = ex.getIn().getBody();
		assertTrue(foo instanceof QueryPersistedEvent);
	}
	
	@Test 
	public void questionStateChangeTest() throws Exception
	{
		QuestionStateChangeEvent event = new QuestionStateChangeEvent();
		QuestionReuseStateChangeRequest request = new QuestionReuseStateChangeRequest();
		when(this.processingService.processQuestionStateChangeEvent(any(QuestionStateChangeEvent.class))).thenReturn(request);
		when(this.processingService.processAnswerStateChangeEvent(any(AnswerStateChangeEvent.class))).thenThrow(new IllegalArgumentException("Wrong method"));
		when(this.processingService.processAnswerApprovedEvent(any(AnswerApprovedEvent.class))).thenThrow(new IllegalArgumentException("Wrong method"));
		
		event.setQid(42L);
		request.setQid(42L);
		request.setReuseable(true);
		
		template.sendBody("direct:process.reuse.events", event);
		verify(this.processingService).processQuestionStateChangeEvent(event);
	}
	
	@Test
	public void answerStateChangeTest() throws Exception
	{
		QuestionReuseStateChangeRequest request = new QuestionReuseStateChangeRequest();
		when(this.processingService.processQuestionStateChangeEvent(any(QuestionStateChangeEvent.class))).thenThrow(new IllegalArgumentException("Wrong method"));
		when(this.processingService.processAnswerStateChangeEvent(any(AnswerStateChangeEvent.class))).thenReturn(request);
		when(this.processingService.processAnswerApprovedEvent(any(AnswerApprovedEvent.class))).thenThrow(new IllegalArgumentException("Wrong method"));
		
		AnswerStateChangeEvent event = new AnswerStateChangeEvent();
		event.setAnswerId(42L);
		request.setQid(42L);
		request.setReuseable(true);

		qriStateChange.expectedMessageCount(1);
		template.sendBody("direct:process.reuse.events", event);
		qriStateChange.assertIsSatisfied();

	}
	
	@Test
	public void queryUpdateAnonTest() throws Exception
	{
		QueryUpdateRequest request = new QueryUpdateRequest();
		request.setAction(UpdateType.CHANGE_ANONYMOUS_FLAG.name());
		request.setAnonymous(true);
		request.setQhid(42L);
		request.setUserId(42L);
		
		QueryUpdatedEvent response = new QueryUpdatedEvent();
		response.setAnonymous(true);
		response.setAskerId(42L);
		response.setQueryHistoryId(42L);
		response.setQuestionId(41L);
		
		when(this.queryUpdateService.updateQuery(same(request))).thenReturn(response);
		queryUpdatedEventEndpoint.setExpectedMessageCount(1);
		
		template.sendBody("direct:query.update", request);
		queryUpdatedEventEndpoint.assertIsSatisfied();	
	}

	@Test
	public void illegalQueryUpdateAnonTest() throws Exception
	{
		QueryUpdateRequest request = new QueryUpdateRequest();
		request.setAction(UpdateType.CHANGE_ANONYMOUS_FLAG.name());
		request.setAnonymous(true);
		request.setQhid(42L);
		request.setUserId(42L);
		
		QueryUpdatedEvent response = new QueryUpdatedEvent();

		
		when(this.queryUpdateService.updateQuery(same(request))).thenReturn(response);
		queryUpdatedEventEndpoint.setExpectedMessageCount(0);
		mockReturn.setExpectedMessageCount(1);
		
		Map<String,Object> headers = ImmutableMap.<String, Object>builder()
				.put("routingSlipHeader", "mock:return")
				.build();

		queryUpdatedEventEndpoint.setResultWaitTime(100);
		mockReturn.setResultWaitTime(100);
		template.sendBodyAndHeaders("direct:query.update", request, headers);
		queryUpdatedEventEndpoint.assertIsSatisfied();
		mockReturn.assertIsSatisfied();
		Exchange exchange = mockReturn.getReceivedExchanges().get(0);
		Message in = exchange.getIn();
		ResponseMessage msg = in.getBody(ResponseMessage.class);
		assertNotNull(msg);
		assertEquals(msg.getCode(), ResponseCode.AUTHENTICATION_ERROR);
		assertFalse(msg.isSuccess());

	}
	
	
	@Override
	public RouteBuilder createRouteUnderTest()
	{
		return new ApplicationRoutes();
	}

	@Override
	public void configBeans(Map<String, Object> beans)
	{
		super.configBeans(beans);
		beans.put("eventCountDao", eventCountDao);
		beans.put("priorityQueueDao", priorityQueueDao);
		beans.put("conversationPersistenceService", conversationPersistenceService);
		beans.put("questionUpdateService", questionUpdateService);
		beans.put("automatedPersistenceService", automatedPersistenceService);
		beans.put("reuseEventProcessingService", processingService);
		beans.put("textIdentifierDao", textIdentifierDAO);
		beans.put("queryUpdateService", queryUpdateService);
	}

	
}
